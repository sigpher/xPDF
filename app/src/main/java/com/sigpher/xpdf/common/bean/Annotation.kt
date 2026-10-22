package com.sigpher.xpdf.common.bean

import org.greenrobot.greendao.annotation.Entity
import org.greenrobot.greendao.annotation.Id

/**
 * 文本标注实体（greenDAO 表 ANNOTATION）。
 *
 * 说明：
 * - 引擎（pdfium 2.0.3）不提供注释写入能力，标注只存在于应用内，渲染时叠加到页面上；
 * - [rects] 存 **页面相对坐标** 的 JSON（每项 [left, top, right, bottom]，取值 0..1），
 *   与渲染时的缩放解耦，换设备/改字号后位置不变；
 * - [pdfId] 对应 [PDF.id]，文档被删除时级联删除。
 *
 * 注意：属性名即列名，改名会破坏已生成 DAO 与本文件的契约。
 */
@Entity
class Annotation() {

    @field:Id(autoincrement = true)
    var id: Long? = null

    /** 所属文档 ID（PDF._id） */
    var pdfId: Long = 0

    /** 从 0 开始的页码 */
    var page: Int = 0

    /** 选区在整页文本中的起始字符索引 */
    var startIndex: Int = 0

    /** 选区结束字符索引（不含） */
    var endIndex: Int = 0

    /** 选中文本 */
    var text: String? = null

    /** 用户笔记 */
    var note: String? = null

    /** 标注颜色（ARGB） */
    var color: Int = 0

    /** 标注类型：0 高亮，1 下划线 */
    var type: Int = 0

    /** 页面相对矩形 JSON：[[l,t,r,b], ...]，取值 0..1 */
    var rects: String? = null

    var createdAt: Long = 0

    fun hasNote(): Boolean = !note.isNullOrEmpty()

    companion object {
        const val TYPE_HIGHLIGHT = 0
        const val TYPE_UNDERLINE = 1

        /** 默认高亮色（半透明黄） */
        const val DEFAULT_HIGHLIGHT_COLOR = 0x66FFD54F.toInt()

        /** 默认下划线色 */
        const val DEFAULT_UNDERLINE_COLOR = 0xFFF9A825.toInt()
    }
}
