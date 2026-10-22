package com.sigpher.xpdf.common

import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.sigpher.xpdf.R

/**
 * @author sigpher
 */
class CoverHolder(itemView: View) : BaseHolder(itemView) {
    val ivCover: ImageView
    val tvTitle: TextView
    val tvProgress: TextView
    override val checkBox: CheckBox

    companion object {
        // AGP 9 默认 non-final 资源 ID，R.layout.* 不再是编译期常量
        val DEFAULT_LAYOUT = R.layout.app_recycler_item_cover
        val DEFAULT_LAYOUT_HORIZONTAL = R.layout.app_recycler_item_cover_horizontal
    }

    init {
        ivCover = itemView.findViewById(R.id.app_iv_cover)
        tvTitle = itemView.findViewById(R.id.app_tv_title)
        tvProgress = itemView.findViewById(R.id.app_tv_progress)
        checkBox = itemView.findViewById(R.id.app_cb)
    }
}