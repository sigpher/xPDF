package com.sigpher.xpdf.preview

import io.legere.pdfiumandroid.api.Bookmark as PdfBookmark

/**
 * @author sigpher
 */
interface IContentFragInterface {
    fun update(collection: MutableCollection<PdfBookmark>)
}