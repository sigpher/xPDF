package com.sigpher.xpdf.main

/**
 * @author sigpher
 */
class PdfDeleteEvent(
        val deleted: List<String> = listOf(),
        val dir: String,
        val isEmpty: Boolean
)