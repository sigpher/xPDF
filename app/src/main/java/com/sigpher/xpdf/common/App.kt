package com.sigpher.xpdf.common

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.multidex.MultiDex
import com.blankj.utilcode.util.SPStaticUtils
import com.github.anzewei.parallaxbacklayout.ParallaxHelper
import com.sigpher.xpdf.common.greendao.DaoMaster
import com.sigpher.xpdf.common.utils.ShortcutUtils

/**
 * @author sigpher
 */
class App : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(base)
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        ShortcutUtils.init(this)
        DBHelper.init(this, AppConfig.DB_NAME)

        val dbVersion = SPStaticUtils.getInt(DB_VERSION)
        if (!isFirstInstall() && dbVersion < DaoMaster.SCHEMA_VERSION) {
            DBHelper.migrate()
            SPStaticUtils.put(DB_VERSION, DaoMaster.SCHEMA_VERSION)
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

        private lateinit var context: Context

        /**
         * 全局 Context（application context）。Kotlin 与 Java 调用方都通过 App.getContext() 访问。
         */
        @JvmStatic
        fun getContext(): Context = context
    }
}
