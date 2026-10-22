package com.sigpher.xpdf.preview

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.blankj.utilcode.util.ConvertUtils
import com.sigpher.xpdf.R

/**
 * 书签角标绘制。
 *
 * 位图只在构造时解码一次，绘制阶段只做坐标计算与 drawBitmap，
 * 避免原先每帧都重新解码位图造成的滑动卡顿。
 *
 * @author sigpher
 */
class BookmarkBadgeRenderer(resources: Resources) {

    private val bitmap: Bitmap? = try {
        BitmapFactory.decodeResource(resources, R.drawable.app_img_bookmark)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private val paint = Paint()

    /**
     * @param canvas PDFView 暴露的画布
     * @param pageWidth 页面原始宽度（px）
     * @param scaleFactor 当前缩放比例，用于把角标贴到放大后页面的右侧
     */
    fun draw(canvas: Canvas?, pageWidth: Float, scaleFactor: Float) {
        val badge = bitmap ?: return
        if (canvas == null || badge.isRecycled) {
            return
        }
        val curPageWidth = pageWidth * scaleFactor
        val margin = (curPageWidth - pageWidth) / 3f
        val left = pageWidth - ConvertUtils.dp2px(36f) - margin
        canvas.drawBitmap(badge, left, 0f, paint)
    }

    /** 页面销毁时释放位图；调用后 [draw] 不再绘制 */
    fun recycle() {
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}
