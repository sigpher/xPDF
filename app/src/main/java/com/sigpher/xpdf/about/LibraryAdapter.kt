package com.sigpher.xpdf.about

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sigpher.xpdf.R
import com.sigpher.xpdf.databinding.AppRecyclerItemLibraryBinding

class LibraryAdapter<T>(
        private val context: Context,
        private val list: List<T>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AppRecyclerItemLibraryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val library = list[position] as Library
        holder as ViewHolder
        holder.binding.appTvName.text = library.name
        holder.binding.appTvAuthor.text = library.author
        holder.binding.appTvDetail.text = library.introduce
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(val binding: AppRecyclerItemLibraryBinding) : RecyclerView.ViewHolder(binding.root)
}
