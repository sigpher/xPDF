package com.sigpher.xpdf.common

import com.sigpher.xpdf.BuildConfig

/**
 * 应用级常量。
 *
 * @author sigpher
 */
object AppConfig {

    const val DB_NAME = "xpdf.db"

    /** FileProvider authority，与清单中 ${applicationId}.fileprovider 保持一致 */
    val AUTHORITY: String = BuildConfig.APPLICATION_ID + ".fileprovider"
}
