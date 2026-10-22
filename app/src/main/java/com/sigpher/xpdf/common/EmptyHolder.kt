package com.sigpher.xpdf.common

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.widgets.ImageTextView

/**
 * 列表空视图 Holder。
 *
 * @author sigpher
 */
class EmptyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    /** 用 @JvmField 暴露字段，Java 侧（如尚未转换的 Adapter）可直接 holder.itvEmpty */
    @JvmField
    val itvEmpty: ImageTextView = itemView.findViewById(R.id.app_itv_placeholder)

    companion object {
        @JvmField
        val DEFAULT_LAYOUT: Int = R.layout.app_recycler_item_emptyview
    }
}
