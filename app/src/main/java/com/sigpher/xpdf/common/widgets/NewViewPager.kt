package com.sigpher.xpdf.common.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/**
 * 支持整体禁用的 ViewPager（禁用后不再拦截触摸，子 View 可正常处理事件）。
 *
 * 注意：`setScrollable(Boolean)` 必须保留为普通函数，MainActivity 仍以函数形式调用。
 *
 * @author sigpher
 */
class NewViewPager : ViewPager {

    private var scrollable = true

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // 必须返回 false ，否则子 View 无法处理触摸事件
        return scrollable && super.onTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // 必须返回 false ，否则子 View 无法处理触摸事件
        return scrollable && super.onInterceptTouchEvent(ev)
    }

    /**
     * 是否允许左右滑动
     */
    fun setScrollable(enable: Boolean) {
        scrollable = enable
    }
}
