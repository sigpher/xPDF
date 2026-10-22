package com.sigpher.xpdf.main

import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.App
import com.sigpher.xpdf.common.CoverHolder
import com.sigpher.xpdf.common.Settings
import com.sigpher.xpdf.common.adapter.BaseListAdapter
import com.sigpher.xpdf.common.adapter.CoverGridRenderer
import com.sigpher.xpdf.common.bean.PDF

/**
 * 单个分组内的书籍列表。
 *
 * @author sigpher
 */
class CollectionAdapter(
        data: MutableList<PDF>
) : BaseListAdapter<PDF, CoverHolder>(
        if (Settings.linearLayout) CoverHolder.DEFAULT_LAYOUT_HORIZONTAL
        else CoverHolder.DEFAULT_LAYOUT,
        data
) {

    override val checkBoxId: Int = R.id.app_cb
    override val emptyText: CharSequence = App.getContext().getString(R.string.app_have_no_all)
    override val emptyIcon: Int = R.drawable.app_img_all

    override fun convert(helper: CoverHolder, item: PDF) {
        helper.tvTitle.text = item.name
        helper.tvProgress.text = mContext.getString(R.string.app_already_read, item.progress)
        CoverGridRenderer.bindCover(helper.ivCover, item.cover)
    }
}
