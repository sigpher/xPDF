package com.sigpher.xpdf.common

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import com.blankj.utilcode.util.SPStaticUtils
import com.github.anzewei.parallaxbacklayout.ParallaxHelper
import com.sigpher.xpdf.common.utils.ShortcutUtils

/**
 * @author sigpher
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        ShortcutUtils.init(this)
        DBHelper.init(this, AppConfig.DB_NAME)

        // 注意：这里的版本与 DaoMaster.SCHEMA_VERSION 解耦。
        // 历史上该标记复用了 schema 版本，导致每次 schema +1 都会把整库 PDF 记录重写一遍
        // （数千次单条写入 + 清空用户缩放比例），启动明显变慢。
        // 只有"数据语义"需要修正时才提升 LEGACY_DATA_VERSION。
        val dataVersion = SPStaticUtils.getInt(DB_VERSION)
        if (!isFirstInstall() && dataVersion in 1 until LEGACY_DATA_VERSION) {
            DBHelper.migrate()
            SPStaticUtils.put(DB_VERSION, LEGACY_DATA_VERSION)
        }

        // 书库数据在 IO 线程加载，主线程不等待；加载完成后由 EventBus 通知刷新
        DataManager.initAsync()
        registerActivityLifecycleCallbacks(ParallaxHelper.getInstance())
    }

    private fun isFirstInstall(): Boolean {
        val packageInfo = try {
            packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        } ?: return false
        return packageInfo.firstInstallTime == packageInfo.lastUpdateTime
    }

    companion object {

        private const val DB_VERSION = "DB_VERSION"

        /**
         * 历史数据语义版本（与 greenDAO schema 版本无关）。
         * 曾经用于 position / scaleFactor 的数据修正，已经修正过的老用户标记为 3。
         */
        private const val LEGACY_DATA_VERSION = 3

        private lateinit var context: Context

        /**
         * 全局 Context（application context）。Kotlin 与 Java 调用方都通过 App.getContext() 访问。
         */
        @JvmStatic
        fun getContext(): Context = context
    }
}
