package com.sigpher.xpdf.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ConvertUtils

/**
 * 三列网格的左右间距。
 *
 * @author sigpher
 */
class XGridDecoration : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val offset = ConvertUtils.dp2px(8f)
        outRect.left = offset
        outRect.right = offset
    }
}
