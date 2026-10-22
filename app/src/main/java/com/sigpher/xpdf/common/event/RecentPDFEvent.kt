package com.sigpher.xpdf.common.event

/**
 * 最近阅读列表发生变化。
 *
 * @param isFromPreviewActivity 为 true 表示事件由阅读器发出（返回书架后需要立即刷新界面），
 * 否则只更新数据、等下一次进入界面时再刷新
 * @author sigpher
 */
data class RecentPDFEvent(
        val isFromPreviewActivity: Boolean = false
)
