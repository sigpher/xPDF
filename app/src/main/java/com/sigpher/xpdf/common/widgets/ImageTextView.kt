package com.sigpher.xpdf.common.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.sigpher.xpdf.R

/**
 * 支持四向图标（含 tint 与尺寸）的 TextView。
 *
 * 注意：`setIconXxx(Int)` 以资源 id 设置图标的入口必须保留为普通函数——它和 `iconXxx` 属性的
 * `setIconXxx(Drawable)` 构成重载，调用点（如 `setIconTop(R.drawable.xxx)`）依赖该重载。
 *
 * @author sigpher
 */
@SuppressLint("AppCompatCustomView")
class ImageTextView : TextView {

    var iconLeft: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }
    var iconLeftTint: Int = 0
    var iconLeftWidth: Int = 0
    var iconLeftHeight: Int = 0

    var iconRight: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }
    var iconRightTint: Int = 0
    var iconRightWidth: Int = 0
    var iconRightHeight: Int = 0

    var iconTop: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }
    var iconTopTint: Int = 0
    var iconTopWidth: Int = 0
    var iconTopHeight: Int = 0

    var iconBottom: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }
    var iconBottomTint: Int = 0
    var iconBottomWidth: Int = 0
    var iconBottomHeight: Int = 0

    private var drawFlag = 0

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, -1)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 只在测量完成后应用两次，之后不再重复设置 drawable bounds
        if (drawFlag != 2) {
            applyIconSize()
            drawFlag++
        }
    }

    fun setIconLeft(drawableRes: Int) {
        iconLeft = ContextCompat.getDrawable(context, drawableRes)
    }

    fun setIconRight(drawableRes: Int) {
        iconRight = ContextCompat.getDrawable(context, drawableRes)
    }

    fun setIconTop(drawableRes: Int) {
        iconTop = ContextCompat.getDrawable(context, drawableRes)
    }

    fun setIconBottom(drawableRes: Int) {
        iconBottom = ContextCompat.getDrawable(context, drawableRes)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ImageTextView)
        // left
        iconLeft = ta.getDrawable(R.styleable.ImageTextView_iconLeft)
        iconLeftTint = ta.getColor(R.styleable.ImageTextView_iconLeftTint, -1)
        iconLeftWidth = ta.getDimension(R.styleable.ImageTextView_iconLeftWidth, -1f).toInt()
        iconLeftHeight = ta.getDimension(R.styleable.ImageTextView_iconLeftHeight, -1f).toInt()
        // right
        iconRight = ta.getDrawable(R.styleable.ImageTextView_iconRight)
        iconRightTint = ta.getColor(R.styleable.ImageTextView_iconRightTint, -1)
        iconRightWidth = ta.getDimension(R.styleable.ImageTextView_iconRightWidth, -1f).toInt()
        iconRightHeight = ta.getDimension(R.styleable.ImageTextView_iconRightHeight, -1f).toInt()
        // top
        iconTop = ta.getDrawable(R.styleable.ImageTextView_iconTop)
        iconTopTint = ta.getColor(R.styleable.ImageTextView_iconTopTint, -1)
        iconTopWidth = ta.getDimension(R.styleable.ImageTextView_iconTopWidth, -1f).toInt()
        iconTopHeight = ta.getDimension(R.styleable.ImageTextView_iconTopHeight, -1f).toInt()
        // bottom
        iconBottom = ta.getDrawable(R.styleable.ImageTextView_iconBottom)
        iconBottomTint = ta.getColor(R.styleable.ImageTextView_iconBottomTint, -1)
        iconBottomWidth = ta.getDimension(R.styleable.ImageTextView_iconBottomWidth, -1f).toInt()
        iconBottomHeight = ta.getDimension(R.styleable.ImageTextView_iconBottomHeight, -1f).toInt()
        ta.recycle()
        // minSdk 21 起 Drawable.setTint 始终可用
        when {
            iconLeft != null && iconLeftTint != -1 -> iconLeft!!.setTint(iconLeftTint)
            iconRight != null && iconRightTint != -1 -> iconRight!!.setTint(iconRightTint)
            iconTop != null && iconTopTint != -1 -> iconTop!!.setTint(iconTopTint)
            iconBottom != null && iconBottomTint != -1 -> iconBottom!!.setTint(iconBottomTint)
        }
    }

    private fun applyIconSize() {
        when {
            iconLeftWidth != -1 && iconLeftHeight != -1 ->
                iconLeft?.setBounds(0, 0, iconLeftWidth, iconLeftHeight)
            iconRightWidth != -1 && iconRightHeight != -1 ->
                iconRight?.setBounds(0, 0, iconRightWidth, iconRightHeight)
            iconTopWidth != -1 && iconTopHeight != -1 ->
                iconTop?.setBounds(0, 0, iconTopWidth, iconTopHeight)
            iconBottomWidth != -1 && iconBottomHeight != -1 ->
                iconBottom?.setBounds(0, 0, iconBottomWidth, iconBottomHeight)
            else -> {
                setCompoundDrawablesWithIntrinsicBounds(iconLeft, iconTop, iconRight, iconBottom)
                return
            }
        }
        setCompoundDrawables(iconLeft, iconTop, iconRight, iconBottom)
    }
}
