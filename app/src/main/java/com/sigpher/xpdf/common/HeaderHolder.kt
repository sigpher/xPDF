package com.sigpher.xpdf.common

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sigpher.xpdf.R

/**
 * 搜索页结果数量表头 Holder。
 *
 * @author sigpher
 */
class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    @JvmField
    val tvCount: TextView = itemView.findViewById(R.id.app_tv_count)

    companion object {
        @JvmField
        val DEFAULT_LAYOUT: Int = R.layout.app_recycler_item_search_header
    }
}
