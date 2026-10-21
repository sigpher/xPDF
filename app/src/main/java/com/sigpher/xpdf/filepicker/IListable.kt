package com.sigpher.xpdf.filepicker

import java.io.File

/**
 * @author sigpher
 */
interface IListable {
    fun listFile(path: String?): List<File>
}