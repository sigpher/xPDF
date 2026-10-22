package com.sigpher.xpdf.common

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ConvertUtils
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.adapter.CoverGridRenderer
import com.sigpher.xpdf.common.adapter.ItemTypes
import com.sigpher.xpdf.common.bean.Cover

/**
 * 分组选择对话框（重分组 / 移动到分组）的列表 Adapter。
 *
 * 书封渲染与 [com.sigpher.xpdf.main.AllAdapter] 共用 [CoverGridRenderer]。
 *
 * @author sigpher
 */
class GroupingAdapter(
        private val layoutManager: LinearLayoutManager?,
        private val coverList: List<Cover>,
        private val callback: Callback?,
        private val enableAddNew: Boolean = true
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /** 空列表时只把 spanSize 改成 3，避免覆盖 DialogManager 按线性/网格设定的 lookup */
    private var spanSizePatched = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if (viewType == ItemTypes.TYPE_EMPTY) {
            return EmptyHolder(inflater.inflate(R.layout.app_recycler_item_emptyview, parent, false))
        }
        if (enableAddNew && viewType == ItemTypes.TYPE_ADD_COLLECTION) {
            val layout = if (Settings.linearLayout) CoverHolder.DEFAULT_LAYOUT_HORIZONTAL else CoverHolder.DEFAULT_LAYOUT
            val holder = CoverHolder(inflater.inflate(layout, parent, false))
            holder.tvProgress.visibility = ViewGroup.GONE
            holder.checkBox.visibility = ViewGroup.GONE
            holder.itemView.setOnClickListener { callback?.onAddNewGroup() }
            return holder
        }
        val layout = if (Settings.linearLayout) CollectionHolder.DEFAULT_LAYOUT_HORIZONTAL else CollectionHolder.DEFAULT_LAYOUT
        val holder = CollectionHolder(inflater.inflate(layout, parent, false))
        holder.itemView.setOnClickListener { callback?.onAddToGroup(holder.tvTitle.text.toString()) }
        return holder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EmptyHolder -> {
                holder.itvEmpty.visibility = ViewGroup.VISIBLE
                holder.itvEmpty.setText(R.string.app_have_no_group)
                holder.itvEmpty.setIconTop(R.drawable.app_img_all)
            }

            is CoverHolder -> {
                holder.tvTitle.setText(R.string.app_add_new_group)
                holder.ivCover.scaleType = ImageView.ScaleType.FIT_XY
                holder.ivCover.setImageResource(R.drawable.app_img_add_group)
                holder.ivCover.layoutParams?.height = ConvertUtils.dp2px(152f)
                holder.ivCover.requestLayout()
            }

            is CollectionHolder -> {
                if (coverList.isNotEmpty()) {
                    CoverGridRenderer.render(holder, coverList[if (enableAddNew) position - 1 else position])
                }
            }
        }
    }

    override fun getItemCount(): Int {
        if (coverList.isEmpty()) {
            patchSpanSize()
            return 1
        }
        return if (enableAddNew) 1 + coverList.size else coverList.size
    }

    override fun getItemViewType(position: Int): Int {
        if (coverList.isEmpty()) {
            return ItemTypes.TYPE_EMPTY
        }
        return if (position == 0) ItemTypes.TYPE_ADD_COLLECTION else super.getItemViewType(position)
    }

    private fun patchSpanSize() {
        if (spanSizePatched) {
            return
        }
        val gridLayoutManager = layoutManager as? GridLayoutManager ?: return
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int = if (coverList.isEmpty()) 3 else 1
        }
        spanSizePatched = true
    }

    interface Callback {

        fun onAddNewGroup()

        fun onAddToGroup(dir: String)
    }
}
