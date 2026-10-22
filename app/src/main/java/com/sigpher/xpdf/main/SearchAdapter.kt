package com.sigpher.xpdf.main

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.*
import com.sigpher.xpdf.common.adapter.ItemTypes
import com.sigpher.xpdf.common.adapter.KeywordFilter
import com.sigpher.xpdf.common.bean.PDF
import com.sigpher.xpdf.common.event.RecentPDFEvent
import com.sigpher.xpdf.preview.PreviewActivity
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.TimeUtils
import com.bumptech.glide.Glide
import org.greenrobot.eventbus.EventBus
import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * @author sigpher
 */
internal class SearchAdapter(
        sourceList: List<PDF>,
        canSelect: Boolean
) : AbstractAdapter<PDF>(sourceList, canSelect), Filterable {

    private val recentPDFEvent: RecentPDFEvent = RecentPDFEvent()
    private val filterList: MutableList<PDF> = ArrayList()

    /** 关键字 + 反向过滤，与列表解耦，便于复用 */
    private val keywordFilter = KeywordFilter(sourceList) { it.name.orEmpty() }

    init {
        keywordFilter.onFilter = { result ->
            filterList.clear()
            filterList.addAll(result)
            notifyDataSetChanged()
        }
    }

    fun update() {
        DataManager.updatePDFs()
    }

    val isEmpty: Boolean
        get() = filterList.isEmpty()

    fun setInverse(inverse: Boolean) {
        keywordFilter.inverse = inverse
    }

    override fun getFilter(): Filter = keywordFilter

    override fun createHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == ItemTypes.TYPE_EMPTY) {
            val itemView = inflater.inflate(EmptyHolder.DEFAULT_LAYOUT, parent, false)
            return EmptyHolder(itemView)
        } else if (viewType == ItemTypes.TYPE_HEADER) {
            val itemView = inflater.inflate(HeaderHolder.DEFAULT_LAYOUT, parent, false)
            return HeaderHolder(itemView)
        }
        val itemView = inflater.inflate(
                if (Settings.linearLayout) CoverHolder.DEFAULT_LAYOUT_HORIZONTAL
                else CoverHolder.DEFAULT_LAYOUT,
                parent,
                false
        )
        return CoverHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun bindHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (viewHolder is CoverHolder && position < itemCount) {
            val pdf = filterList[position - 1]
            val cover = pdf.cover
            val bookName = pdf.name
            viewHolder.tvTitle.text = bookName
            viewHolder.tvProgress.text = context.getString(R.string.app_already_read, pdf.progress)
            if (!StringUtils.isEmpty(cover)) {
                Glide.with(context).load(cover).into(viewHolder.ivCover)
            } else {
                viewHolder.ivCover.scaleType = ImageView.ScaleType.FIT_XY
                viewHolder.ivCover.setImageResource(R.drawable.app_img_none_cover)
            }
            handleCheckBox(viewHolder.checkBox, position)
        } else if (viewHolder is HeaderHolder) {
            val count = filterList.size
            viewHolder.tvCount.text = context.getString(R.string.app_total, count)
        } else if (viewHolder is EmptyHolder) {
            viewHolder.itvEmpty.visibility = View.VISIBLE
            viewHolder.itvEmpty.setText(R.string.app_have_no_file)
            viewHolder.itvEmpty.setIconTop(R.drawable.app_img_file)
        }
    }

    override fun bindHolder(viewHolder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        bindHolder(viewHolder, position)
    }

    override fun itemCount(): Int {
        return if (filterList.isEmpty()) {
            1
        } else filterList.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (filterList.isEmpty()) {
            return ItemTypes.TYPE_EMPTY
        }
        return if (position == 0) {
            ItemTypes.TYPE_HEADER
        } else ItemTypes.TYPE_CONTENT
    }

    override fun onTap(viewHolder: RecyclerView.ViewHolder?, position: Int) {
        if (viewHolder is CoverHolder) {
            val pdf = filterList[position - 1]
            val cur = System.currentTimeMillis()
            @SuppressLint("SimpleDateFormat") val df: DateFormat = SimpleDateFormat("yyyyMMddHHmmss")
            pdf.latestRead = TimeUtils.millis2String(cur, df).toLong()
            DBHelper.updatePDF(pdf)
            DBHelper.insertRecent(pdf)
            DataManager.updateRecentPDFs()
            PreviewActivity.start(context, pdf)
            EventBus.getDefault().post(recentPDFEvent)
        }
    }

    override fun checkCurrent(viewHolder: RecyclerView.ViewHolder?, position: Int) {}
}