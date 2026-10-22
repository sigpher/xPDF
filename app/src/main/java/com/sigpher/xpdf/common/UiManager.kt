package com.sigpher.xpdf.common

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ToastUtils
import com.sigpher.base.util.StatusBarUtils
import com.sigpher.xpdf.R

/**
 * 状态栏/导航栏与轻提示的统一入口。
 *
 * @author sigpher
 */
object UiManager {

    fun setNavigationBarColor(activity: Activity, color: Int) {
        activity.window.navigationBarColor = color
    }

    fun setStatusBar(activity: Activity, toolbar: Toolbar) {
        toolbar.setPadding(0, ConvertUtils.dp2px(25f), 0, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StatusBarUtils.setTransparent(activity, true)
        } else {
            StatusBarUtils.setTranslucent(activity, 60)
        }
    }

    fun setTransparentStatusBar(activity: Activity) {
        StatusBarUtils.setTransparent(activity)
    }

    fun setTranslucentStatusBar(activity: Activity) {
        StatusBarUtils.setTranslucent(activity, 100)
    }

    fun setTransparentNavigationBar(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.navigationBarColor = Color.TRANSPARENT
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
    }

    fun setTransparentStatusBar(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.navigationBarColor = Color.TRANSPARENT
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    fun setTranslucentStatusBar(activity: Activity, @IntRange(from = 0, to = 255) alpha: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = activity.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = Color.argb(alpha, 0, 0, 0)
        } else {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    fun showShort(text: CharSequence) {
        ToastUtils.setGravity(Gravity.BOTTOM, 0, 250)
        val toastView = ToastUtils.showCustomShort(R.layout.app_toast)
        toastView.findViewById<TextView>(R.id.app_tv).text = text
    }

    fun showShort(@StringRes res: Int) {
        ToastUtils.setGravity(Gravity.BOTTOM, 0, 250)
        val toastView = ToastUtils.showCustomShort(R.layout.app_toast)
        toastView.findViewById<TextView>(R.id.app_tv).setText(res)
    }

    fun showCenterShort(@StringRes res: Int) {
        ToastUtils.setGravity(Gravity.CENTER, 0, 0)
        val toastView = ToastUtils.showCustomShort(R.layout.app_toast)
        toastView.findViewById<TextView>(R.id.app_tv).setText(res)
    }
}
