package com.sigpher.xpdf.common.event

/**
 * 单本书的导入进度；[stop] 由界面置为 true 时导入循环会尽快退出。
 *
 * @author sigpher
 */
class ImportEvent {

    var name: String? = null

    var curProgress: Int = 0

    var totalProgress: Int = 0

    var stop: Boolean = false
}
