package com.sigpher.xpdf.main

import android.net.Uri
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
    /**
     * 导入系统选择器返回的文档（一次可选多个）
     */
    abstract fun importUris(uris: List<Uri>, groupName: String)

    /**
     * 导入一个目录树下的全部 PDF
     */
    abstract fun importTree(treeUri: Uri)
}
