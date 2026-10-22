package com.sigpher.xpdf.common

import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.widgets.BorderImageView

/**
 * @author sigpher
 */
class CollectionHolder(itemView: View) : BaseHolder(itemView) {
    val ivCover1: BorderImageView
    val ivCover2: BorderImageView
    val ivCover3: BorderImageView
    val ivCover4: BorderImageView
    val tvTitle: TextView
    val tvCount: TextView
    override val checkBox: CheckBox

    /** 按索引取封面图，避免调用方按 1~4 写四份分支 */
    fun coverAt(index: Int): ImageView = when (index) {
        0 -> ivCover1
        1 -> ivCover2
        2 -> ivCover3
        else -> ivCover4
    }

    companion object {
        // AGP 9 默认 non-final 资源 ID，R.layout.* 不再是编译期常量
        val DEFAULT_LAYOUT = R.layout.app_recycler_item_collection
        val DEFAULT_LAYOUT_HORIZONTAL = R.layout.app_recycler_item_collection_horizontal
    }

    init {
        ivCover1 = itemView.findViewById(R.id.app_iv_1)
        ivCover2 = itemView.findViewById(R.id.app_iv_2)
        ivCover3 = itemView.findViewById(R.id.app_iv_3)
        ivCover4 = itemView.findViewById(R.id.app_iv_4)
        tvTitle = itemView.findViewById(R.id.app_tv_title)
        tvCount = itemView.findViewById(R.id.app_tv_count)
        checkBox = itemView.findViewById(R.id.app_cb)
    }
}