package com.sigpher.xpdf.filepicker

import androidx.recyclerview.widget.RecyclerView

/**
 * @author sigpher
 */
abstract class AbstractAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    abstract fun selectAll(selectAll: Boolean)
    abstract fun reset(): Boolean
}