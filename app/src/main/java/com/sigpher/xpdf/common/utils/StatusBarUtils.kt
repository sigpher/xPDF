package com.sigpher.xpdf.common.utils

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi

/**
 * 状态栏工具，只保留 app 实际使用的方法。
 *
 * 内联自 sigpher/Base 的 `StatusBarUtils`（Apache License 2.0），
 * 其透明/半透明状态栏实现参考 jaeger/StatusBarUtil。
 *
 * @author Aaron aaronzheng9603@gmail.com
 * @author sigpher
 */
object StatusBarUtils {

    /**
     * 状态栏透明，内容延伸到状态栏下方
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun setTransparent(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            activity.window.statusBarColor = Color.TRANSPARENT
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        } else {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    /**
     * 状态栏透明，并指定状态栏图标是否为深色（lightStatusBar 为 true 时图标变黑）
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun setTransparent(activity: Activity, lightStatusBar: Boolean) {
        setTransparent(activity)
        activity.window.decorView.systemUiVisibility = if (lightStatusBar) {
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    /**
     * 状态栏半透明黑
     *
     * @param alpha 0~255，透明度
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun setTranslucent(activity: Activity, @IntRange(from = 0, to = 255) alpha: Int = 60) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            activity.window.statusBarColor = Color.argb(alpha, 0, 0, 0)
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        } else {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }
}
