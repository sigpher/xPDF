package com.sigpher.xpdf.preview

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.github.barteksc.pdfviewer.text.TextSelection
import com.sigpher.xpdf.common.bean.Annotation
import org.json.JSONArray

/**
 * 标注叠加渲染。
 *
 * 由 [com.github.barteksc.pdfviewer.PDFView.setOnPageOverlayListener] 在"页面左上角为原点"的
 * canvas 上回调，因此这里只需把 0..1 的相对矩形乘以当前页宽高即可，天然适配缩放与两种翻页模式。
 */
class AnnotationOverlayRenderer(resources: Resources) {

    private val density = resources.displayMetrics.density

    private val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val notePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0xCCFF8F00.toInt()
    }

    fun draw(canvas: Canvas?, pageWidth: Float, pageHeight: Float, annotations: List<Annotation>?) {
        if (canvas == null || annotations.isNullOrEmpty() || pageWidth <= 0f || pageHeight <= 0f) {
            return
        }
        for (annotation in annotations) {
            val rects = parseRects(annotation.rects)
            if (rects.isEmpty()) {
                continue
            }
            if (annotation.type == Annotation.TYPE_UNDERLINE) {
                fillPaint.color = if (annotation.color == 0) {
                    Annotation.DEFAULT_UNDERLINE_COLOR
                } else {
                    annotation.color
                }
                val thickness = (2f * density).coerceAtMost(pageHeight * 0.01f)
                for (rect in rects) {
                    val bottom = rect.bottom * pageHeight
                    canvas.drawRect(rect.left * pageWidth, bottom - thickness,
                            rect.right * pageWidth, bottom, fillPaint)
                }
            } else {
                fillPaint.color = if (annotation.color == 0) {
                    Annotation.DEFAULT_HIGHLIGHT_COLOR
                } else {
                    annotation.color
                }
                for (rect in rects) {
                    canvas.drawRect(rect.left * pageWidth, rect.top * pageHeight,
                            rect.right * pageWidth, rect.bottom * pageHeight, fillPaint)
                }
            }
            if (annotation.hasNote()) {
                // 右上角小圆点表示该标注带笔记
                val first = rects[0]
                canvas.drawCircle(first.right * pageWidth - 3f * density,
                        first.top * pageHeight + 3f * density, 2.5f * density, notePaint)
            }
        }
    }

    companion object {

        /** 解析 [[l,t,r,b], ...] 形式的相对矩形，非法数据返回空列表 */
        @JvmStatic
        fun parseRects(json: String?): List<RectF> {
            if (json.isNullOrEmpty()) {
                return emptyList()
            }
            return try {
                val array = JSONArray(json)
                val result = ArrayList<RectF>(array.length())
                for (i in 0 until array.length()) {
                    val item = array.optJSONArray(i) ?: continue
                    if (item.length() < 4) {
                        continue
                    }
                    result.add(RectF(
                            item.getDouble(0).toFloat(),
                            item.getDouble(1).toFloat(),
                            item.getDouble(2).toFloat(),
                            item.getDouble(3).toFloat()))
                }
                result
            } catch (e: Exception) {
                emptyList()
            }
        }

        /** 把选区（页面 point 坐标）转成 0..1 的相对矩形 */
        @JvmStatic
        fun toRelativeRects(selection: TextSelection): List<RectF> {
            val width = selection.pageWidthPoint.toFloat()
            val height = selection.pageHeightPoint.toFloat()
            if (width <= 0f || height <= 0f) {
                return emptyList()
            }
            return selection.rects.mapNotNull { rect ->
                if (rect == null || rect.width() <= 0f || rect.height() <= 0f) {
                    null
                } else {
                    RectF((rect.left / width).coerceIn(0f, 1f),
                            (rect.top / height).coerceIn(0f, 1f),
                            (rect.right / width).coerceIn(0f, 1f),
                            (rect.bottom / height).coerceIn(0f, 1f))
                }
            }
        }

        @JvmStatic
        fun toJson(rects: List<RectF>): String {
            val array = JSONArray()
            for (rect in rects) {
                array.put(JSONArray().apply {
                    put(rect.left.toDouble())
                    put(rect.top.toDouble())
                    put(rect.right.toDouble())
                    put(rect.bottom.toDouble())
                })
            }
            return array.toString()
        }
    }
}
