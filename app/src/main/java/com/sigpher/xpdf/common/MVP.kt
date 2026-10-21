package com.sigpher.xpdf.common

/**
 * @author sigpher
 */
interface IModel

interface IView

abstract class IPresenter<V: IView>(protected var view: V)