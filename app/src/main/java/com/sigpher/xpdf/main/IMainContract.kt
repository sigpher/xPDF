package com.sigpher.xpdf.main

import androidx.annotation.StringRes
import com.sigpher.xpdf.common.IPresenter
import com.sigpher.xpdf.common.IView

/**
 * @author sigpher
 */
interface IMainView : IView {
    fun onShowMessage(@StringRes stringId: Int)
    fun onShowLoading()
    fun onHideLoading()
    fun onUpdate()
}

abstract class IMainPresenter(view: IMainView) : IPresenter<IMainView>(view) {
    abstract fun insertPDF(paths: List<String>?, type: Int?, groupName: String?)
}