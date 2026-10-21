package com.sigpher.xpdf.common;

import com.sigpher.xpdf.BuildConfig;

/**
 * @author sigpher
 */
public interface AppConfig {

    String DB_NAME   = "xpdf.db";
    String AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider";
}
