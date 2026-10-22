package com.sigpher.xpdf.common

/**
 * @author sigpher
 */
interface IModel

interface IView

/**
 * Presenter 基类：显式持有 View，并支持 [detach] 解绑，
 * 避免 Activity 销毁后协程/回调仍通过 Presenter 访问已销毁的界面。
 */
abstract class IPresenter<V : IView>(view: V) {

    private var attachedView: V? = view

    protected val view: V
        get() = checkNotNull(attachedView) { "Presenter 已 detach，不应再访问 View" }

    /** 与 View 解绑，重复调用安全 */
    open fun detach() {
        attachedView = null
    }
}
