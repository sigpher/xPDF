package com.sigpher.xpdf.common.utils

import android.os.SystemClock
import android.util.Log
import android.view.View

/**
 * 防止点击抖动打开多个页面：两次点击间隔小于 [clickInterval] 时不予响应。
 *
 * 内联自 sigpher/Base 的 `OnClickListenerImpl`（Apache License 2.0）。
 *
 * @author Aaron aaronzheng9603@gmail.com
 */
abstract class OnClickListenerImpl(
    private var clickInterval: Long = DEFAULT_CLICK_INTERVAL
) : View.OnClickListener {

    private var lastClickTime = 0L

    override fun onClick(view: View) {
        val nowClickTime = SystemClock.elapsedRealtime()
        val interval = nowClickTime - lastClickTime
        if (lastClickTime == 0L || interval > clickInterval) {
            onViewClick(view, interval)
            lastClickTime = nowClickTime
        } else {
            Log.e(TAG, "点击间隔不得小于${clickInterval}毫秒，否则不予响应。\n当前间隔: $interval")
        }
    }

    /**
     * @param interval 与上一次有效点击的间隔（毫秒），首次点击时无意义
     */
    abstract fun onViewClick(v: View, interval: Long)

    companion object {
        private const val TAG = "OnClickListenerImpl"
        private const val DEFAULT_CLICK_INTERVAL = 1000L
    }
}
