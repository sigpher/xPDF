package com.sigpher.xpdf.common

import android.view.View
import android.widget.CheckBox
import com.chad.library.adapter.base.BaseViewHolder

/**
 * @author sigpher
 */
abstract class BaseHolder(itemView: View) : BaseViewHolder(itemView) {

    abstract val checkBox: CheckBox
}