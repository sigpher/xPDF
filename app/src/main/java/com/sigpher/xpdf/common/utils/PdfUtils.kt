package com.sigpher.xpdf.common.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.blankj.utilcode.util.StringUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * PDF 相关工具：统一只读打开、及时关闭 fd，避免导入时重复打开文件。
 *
 * @author sigpher
 */
object PdfUtils {

    /** 封面缩略图宽度（列表展示足够，避免整屏大图） */
    private const val THUMB_WIDTH = 360

    private const val JPEG_QUALITY = 80

    /**
     * 打开 PDF（调用方负责 close，PdfRenderer 关闭时会一并关闭 fd）
     */
    @JvmStatic
    fun openRenderer(path: String?): PdfRenderer? {
        if (StringUtils.isEmpty(path)) {
            return null
        }
        var fd: ParcelFileDescriptor? = null
        return try {
            val descriptor = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
            fd = descriptor
            val renderer = PdfRenderer(descriptor)
            fd = null // 所有权移交给 PdfRenderer
            renderer
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            closeQuietly(fd)
        }
    }

    /**
     * 渲染指定页为缩略图，背景填白（PDF 页面本身透明）
     */
    @JvmStatic
    fun renderPage(renderer: PdfRenderer?, pageIndex: Int): Bitmap? {
        if (renderer == null || pageIndex < 0 || pageIndex >= renderer.pageCount) {
            return null
        }
        var page: PdfRenderer.Page? = null
        return try {
            page = renderer.openPage(pageIndex)
            val width = Math.max(1, Math.min(THUMB_WIDTH, page.width))
            val height = Math.max(1, (width * (page.height * 1.0f / page.width)).toInt())
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            closeQuietly(page)
        }
    }

    @JvmStatic
    fun pdfToBitmap(path: String?, curPage: Int): Bitmap? {
        val renderer = openRenderer(path)
        return try {
            renderPage(renderer, curPage)
        } finally {
            renderer?.closeQuietly()
        }
    }

    /**
     * 保存封面为 JPEG（列表只用于展示，不需要无损 PNG）
     */
    @JvmStatic
    fun saveBitmap(bitmap: Bitmap?, savePath: String?): String? {
        if (bitmap == null || StringUtils.isEmpty(savePath)) {
            return null
        }
        val file = File(savePath)
        if (file.exists()) {
            bitmap.recycle()
            return savePath
        }
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
            }
            savePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            bitmap.recycle()
        }
    }

    @JvmStatic
    fun getPdfTotalPage(path: String?): Int {
        val renderer = openRenderer(path)
        return try {
            renderer?.pageCount ?: 0
        } finally {
            renderer?.closeQuietly()
        }
    }

    @JvmStatic
    fun getPdfWidth(path: String?, page: Int): Int {
        val renderer = openRenderer(path)
        var pdfPage: PdfRenderer.Page? = null
        return try {
            if (renderer == null || page < 0 || page >= renderer.pageCount) {
                0
            } else {
                pdfPage = renderer.openPage(page)
                pdfPage.width
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        } finally {
            closeQuietly(pdfPage)
            renderer?.closeQuietly()
        }
    }

    @JvmStatic
    fun getPdfHeight(path: String?, page: Int): Int {
        val renderer = openRenderer(path)
        var pdfPage: PdfRenderer.Page? = null
        return try {
            if (renderer == null || page < 0 || page >= renderer.pageCount) {
                0
            } else {
                pdfPage = renderer.openPage(page)
                pdfPage.height
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        } finally {
            closeQuietly(pdfPage)
            renderer?.closeQuietly()
        }
    }

    private fun PdfRenderer.closeQuietly() {
        try {
            close()
        } catch (ignored: Exception) {
        }
    }

    private fun closeQuietly(page: PdfRenderer.Page?) {
        if (page == null) {
            return
        }
        try {
            page.close()
        } catch (ignored: Exception) {
        }
    }

    private fun closeQuietly(fd: ParcelFileDescriptor?) {
        if (fd == null) {
            return
        }
        try {
            fd.close()
        } catch (ignored: IOException) {
        }
    }
}
