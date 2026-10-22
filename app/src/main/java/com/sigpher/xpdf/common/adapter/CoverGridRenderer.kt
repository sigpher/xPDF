package com.sigpher.xpdf.common.adapter

import android.view.View
import android.widget.ImageView
import com.blankj.utilcode.util.StringUtils
import com.sigpher.base.image.DefaultOption
import com.sigpher.base.image.ImageLoader
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.CollectionHolder
import com.sigpher.xpdf.common.bean.Cover

/**
 * 书封渲染：分组卡片（1~4 张封面拼图）与单张封面共用一套逻辑，
 * 供 [com.sigpher.xpdf.main.AllAdapter]、[com.sigpher.xpdf.main.CollectionAdapter]
 * 与分组对话框的 Adapter 复用。
 *
 * @author sigpher
 */
object CoverGridRenderer {

    /** 渲染分组卡片：标题、书籍数量与最多 4 张封面 */
    fun render(holder: CollectionHolder, cover: Cover) {
        holder.tvTitle.text = cover.name
        holder.tvCount.text = holder.tvTitle.context.getString(R.string.app_total, cover.count)
        bindVisibility(holder, cover.count)
        val covers = cover.coverList
        val limit = minOf(cover.count, covers.size, MAX_GRID_COUNT)
        for (index in 0 until limit) {
            bindCover(holder.coverAt(index), covers[index])
        }
    }

    /** 单张封面：空路径时显示占位图 */
    fun bindCover(ivCover: ImageView, path: String?) {
        if (!StringUtils.isEmpty(path)) {
            ImageLoader.load(ivCover.context, DefaultOption.Builder(path).into(ivCover))
        } else {
            ivCover.scaleType = ImageView.ScaleType.FIT_XY
            ivCover.setImageResource(R.drawable.app_img_none_cover)
        }
    }

    private fun bindVisibility(holder: CollectionHolder, count: Int) {
        for (index in 0 until MAX_GRID_COUNT) {
            holder.coverAt(index).visibility = if (count > index) View.VISIBLE else View.INVISIBLE
        }
    }

    /** 分组卡片最多展示的封面数 */
    private const val MAX_GRID_COUNT = 4
}
