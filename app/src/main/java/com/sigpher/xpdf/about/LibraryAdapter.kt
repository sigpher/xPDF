package com.sigpher.xpdf.about

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sigpher.xpdf.R
import kotlinx.android.synthetic.main.app_recycler_item_library.view.*

class LibraryAdapter<T>(
        private val context: Context,
        private val list: List<T>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.app_recycler_item_library, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val library = list[position] as Library
        holder as ViewHolder
        holder.itemView.app_tv_name.text = library.name
        holder.itemView.app_tv_author.text = library.author
        holder.itemView.app_tv_detail.text = library.introduce
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
