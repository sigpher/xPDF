package com.sigpher.xpdf.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ConvertUtils

/**
 * 书库列表的上下间距：线性布局用小间距，网格布局首尾行额外留白。
 *
 * @author sigpher
 */
class YGridDecoration : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (Settings.linearLayout) {
            outRect.top = ConvertUtils.dp2px(6f)
            outRect.bottom = ConvertUtils.dp2px(6f)
            return
        }
        val count = state.itemCount
        val pos = parent.getChildAdapterPosition(view)
        if (pos < 3) {
            outRect.top = ConvertUtils.dp2px(8f)
        } else if (pos >= count - 3) {
            outRect.top = ConvertUtils.dp2px(24f)
            outRect.bottom = ConvertUtils.dp2px(8f)
        } else {
            outRect.top = ConvertUtils.dp2px(24f)
        }
    }
}
