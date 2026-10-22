package com.sigpher.xpdf.common.event

/**
 * 分组内书籍发生变化（重命名、删除、调整顺序等），通知「全部文档」列表。
 *
 * @param isEmpty 该分组是否已没有书籍
 * @param dir 分组（目录）名
 * @author sigpher
 */
data class AllEvent(
        val isEmpty: Boolean = false,
        val dir: String? = null
)
