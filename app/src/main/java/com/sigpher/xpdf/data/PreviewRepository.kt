package com.sigpher.xpdf.data

import com.sigpher.xpdf.common.DBHelper
import com.sigpher.xpdf.common.DataManager
import com.sigpher.xpdf.common.bean.PDF
import java.text.NumberFormat

/**
 * 阅读进度的持久化出口：把 Activity 里的「拼字段 + 写库 + 刷新缓存」收敛到一处，
 * 后续换成 suspend + IO 线程时只需改这里。
 *
 * @author sigpher
 */
object PreviewRepository {

    /**
     * 保存阅读进度、书签与缩放比例。
     *
     * @param pdf 当前文档，为空（外部打开且未入库）时直接返回
     * @param curPage 当前页（0 基）
     * @param totalPage 总页数，用于计算百分比
     * @param bookmarkJson 书签列表的 JSON，由调用方从 Gson 序列化
     * @param scaleFactor 当前缩放比例
     */
    @JvmStatic
    fun saveProgress(
            pdf: PDF?,
            curPage: Int,
            totalPage: Int,
            bookmarkJson: String?,
            scaleFactor: Float
    ) {
        pdf ?: return
        pdf.curPage = curPage
        pdf.progress = getPercent(curPage + 1, totalPage)
        pdf.bookmark = bookmarkJson
        pdf.scaleFactor = scaleFactor
        DBHelper.updatePDF(pdf)
        DataManager.updatePDFs()
    }

    /**
     * 获取阅读进度的百分比，保留 1 位小数（与历史数据格式保持一致）
     */
    private fun getPercent(percent: Int, total: Int): String {
        if (total <= 0) {
            return "0%"
        }
        val numberFormat = NumberFormat.getInstance()
        numberFormat.maximumFractionDigits = 1
        val result = numberFormat.format(percent.toFloat() / total.toFloat() * 100f)
        return "$result%"
    }
}
