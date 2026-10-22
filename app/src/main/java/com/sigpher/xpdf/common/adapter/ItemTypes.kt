package com.sigpher.xpdf.common.adapter

/**
 * 列表 viewType 常量，供所有 Adapter 共用，避免各 Adapter 各自定义导致取值冲突。
 *
 * @author sigpher
 */
object ItemTypes {
    /** 普通内容项（BaseQuickAdapter 内部默认值，保持不变） */
    const val TYPE_NORMAL = 0

    /** 空数据占位 */
    const val TYPE_EMPTY = 1

    /** 内容项（非 BaseQuickAdapter 系） */
    const val TYPE_CONTENT = 2

    /** 列表头部 */
    const val TYPE_HEADER = 3

    /** 分组对话框中的「新建分组」占位项 */
    const val TYPE_ADD_COLLECTION = 4
}
