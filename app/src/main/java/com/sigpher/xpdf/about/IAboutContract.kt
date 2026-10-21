package com.sigpher.xpdf.about

import com.sigpher.xpdf.common.IPresenter
import com.sigpher.xpdf.common.IView

/**
 * @author sigpher
 */
interface IAboutView : IView {
    fun onShowLibrary(list: List<Library>)
}

abstract class IAboutPresenter(view: IAboutView) : IPresenter<IAboutView>(view) {
    abstract fun requestLibrary(name: Array<String>, author: Array<String>, introduce: Array<String>)
}