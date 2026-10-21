package com.sigpher.xpdf.preview

import com.shockwave.pdfium.PdfDocument

/**
 * @author sigpher
 */
interface IContentFragInterface {
    fun update(collection: MutableCollection<PdfDocument.Bookmark>)
}