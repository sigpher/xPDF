package com.sigpher.xpdf.common.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * 剪贴板写入。
 *
 * 项目使用的 utilcodex 1.23.7 尚未提供 ClipboardUtils，这里只做最小封装，
 * 并统一处理系统服务缺失（极少数定制 ROM）的情况。
 */
object ClipboardHelper {

    fun copy(context: Context, text: String) {
        if (text.isEmpty()) {
            return
        }
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        manager.setPrimaryClip(ClipData.newPlainText("xPDF", text))
    }
}
