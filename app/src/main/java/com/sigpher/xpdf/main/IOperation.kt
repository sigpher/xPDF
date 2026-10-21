package com.sigpher.xpdf.main

/**
 * @author sigpher
 */
interface IOperation {
    fun createShortcut()
    fun showExport(): Boolean
    fun delete(deleteLocal: Boolean)
    fun selectAll(selectAll: Boolean)
    fun cancelSelect()
    fun deleteDescription(): String?
    fun localDeleteVisibility(): Int
}