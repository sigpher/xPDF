package com.sigpher.xpdf.preview

/**
 * 书签。以 JSON 形式存放在 PDF.bookmark 字段中，因此保留全部默认值以便 Gson 反序列化。
 *
 * @param pageId 书签所在页（0 基）；旧数据可能缺少该字段，默认 0
 * @param title 添加书签时的章节标题
 * @param time 添加时间戳
 * @author sigpher
 */
data class Bookmark(
        var pageId: Int = 0,
        var title: String? = null,
        var time: Long = 0L
)
