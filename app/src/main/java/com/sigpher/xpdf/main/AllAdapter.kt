package com.sigpher.xpdf.main

import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.App
import com.sigpher.xpdf.common.CollectionHolder
import com.sigpher.xpdf.common.Settings
import com.sigpher.xpdf.common.adapter.BaseListAdapter
import com.sigpher.xpdf.common.adapter.CoverGridRenderer
import com.sigpher.xpdf.common.bean.Cover

/**
 * 「全部文档」分组列表。
 *
 * @author sigpher
 */
class AllAdapter(
        data: MutableList<Cover>
) : BaseListAdapter<Cover, CollectionHolder>(
        if (Settings.linearLayout) CollectionHolder.DEFAULT_LAYOUT_HORIZONTAL
        else CollectionHolder.DEFAULT_LAYOUT,
        data
) {

    override val checkBoxId: Int = R.id.app_cb
    override val emptyText: CharSequence = App.getContext().getString(R.string.app_have_no_all)
    override val emptyIcon: Int = R.drawable.app_img_all

    override fun convert(helper: CollectionHolder, item: Cover) {
        CoverGridRenderer.render(helper, item)
    }
}
