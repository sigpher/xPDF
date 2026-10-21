package com.sigpher.xpdf.preview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sigpher.xpdf.R
import com.sigpher.xpdf.databinding.AppRecyclerItemBookmarkBinding
import com.sigpher.xpdf.common.App
import com.sigpher.xpdf.common.EmptyHolder
import com.blankj.utilcode.util.TimeUtils
import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * @author sigpher
 */
internal class BookmarkAdapter(private val mBookmarks: List<Bookmark>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mDateFormat: DateFormat

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        if (viewType == TYPE_EMPTY) {
            val itemView = inflater.inflate(R.layout.app_recycler_item_emptyview, parent, false)
            return EmptyHolder(itemView).apply {
                itvEmpty.setTextColor(App.getContext().resources.getColor(R.color.app_content_bookmark_primary))
                itvEmpty.alpha = 0.6f
            }
        }
        val binding = AppRecyclerItemBookmarkBinding.inflate(inflater, parent, false)
        val holder = ViewHolder(binding)
        holder.itemView.setOnClickListener {
            val pos = holder.adapterPosition
            val bk = mBookmarks[pos]
            (context as IActivityInterface).onJumpTo(bk.pageId)
        }
        return holder
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (mBookmarks.isNotEmpty() && viewHolder is ViewHolder) {
            val bk = mBookmarks[position]
            viewHolder.binding.appTvTitle.text = bk.title
            viewHolder.binding.appTvPageId.text = (bk.pageId + 1).toString()
            val time = TimeUtils.millis2String(bk.time, mDateFormat)
            viewHolder.binding.appTvTime.text = time
        } else if (viewHolder is EmptyHolder) {
            viewHolder.itvEmpty.visibility = View.VISIBLE
            viewHolder.itvEmpty.setText(R.string.app_have_no_add_bookmark)
            viewHolder.itvEmpty.setIconTop(R.drawable.app_ic_bookmark_border_white_24dp)
        }
    }

    override fun getItemCount(): Int {
        return if (mBookmarks.isEmpty()) {
            1
        } else mBookmarks.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (mBookmarks.isEmpty()) {
            TYPE_EMPTY
        } else super.getItemViewType(position)
    }

    internal class ViewHolder(val binding: AppRecyclerItemBookmarkBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val TYPE_EMPTY = 1
    }

    init {
        mDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
    }
}