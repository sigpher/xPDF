package com.sigpher.xpdf.common.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ConvertUtils
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.App

/**
 * 固定 1:1.4 比例、带边框的封面图。
 *
 * 构造器必须保留 `(Context)` 与 `(Context, AttributeSet)` 两个 public 构造器，否则 XML inflate 失败。
 *
 * @author sigpher
 */
class BorderImageView : AppCompatImageView {

    var borderColor: Int = 0
    var borderRadius: Float = 0f
    var isDrawBorder: Boolean = true

    private val paint = Paint()

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
        scaleType = ScaleType.FIT_XY
        paint.color = borderColor
        paint.strokeWidth = ConvertUtils.dp2px(0.5f).toFloat()
        paint.style = Paint.Style.STROKE
    }

    fun clearBorder() {
        isDrawBorder = false
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = measuredWidth
        // 封面固定 1 : 1.4
        setMeasuredDimension(measuredWidth, (measuredWidth * 1.4f).toInt())
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawable != null && isDrawBorder) {
            // 画边框；RectF 在收缩 rect 之前快照，保持与原实现一致的绘制位置
            val rect = canvas.clipBounds
            val rectF = RectF(rect)
            rect.bottom -= 1
            rect.right -= 1
            canvas.drawRoundRect(rectF, borderRadius, borderRadius, paint)
        }
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.BorderImageView)
        borderColor = ta.getColor(R.styleable.BorderImageView_borderColor, BORDER_COLOR)
        borderRadius = ta.getDimension(R.styleable.BorderImageView_borderRadius, BORDER_RADIUS)
        ta.recycle()
    }

    private companion object {

        val BORDER_COLOR = ContextCompat.getColor(App.getContext(), R.color.base_black_hint)

        val BORDER_RADIUS = ConvertUtils.dp2px(1f).toFloat()
    }
}
