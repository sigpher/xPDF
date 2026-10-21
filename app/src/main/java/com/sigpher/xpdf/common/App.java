package com.sigpher.xpdf.common;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.multidex.MultiDex;

import com.sigpher.xpdf.common.greendao.DaoMaster;
import com.sigpher.xpdf.common.utils.ShortcutUtils;
import com.blankj.utilcode.util.SPStaticUtils;
import com.github.anzewei.parallaxbacklayout.ParallaxHelper;

/**
 * @author sigpher
 */
public class App extends Application {

    private static final String DB_VERSION = "DB_VERSION";

    private static Context sContext;

    public static Context getContext() {
        return sContext;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this.getApplicationContext();

        ShortcutUtils.INSTANCE.init(this);
        DBHelper.init(this, AppConfig.DB_NAME);

        int dbVersion = SPStaticUtils.getInt(DB_VERSION);
        if (!isFirstInstall() && dbVersion < DaoMaster.SCHEMA_VERSION) {
            DBHelper.INSTANCE.migrate();
            SPStaticUtils.put(DB_VERSION, DaoMaster.SCHEMA_VERSION);
        }

        DataManager.init();
        registerActivityLifecycleCallbacks(ParallaxHelper.getInstance());
    }

    private boolean isFirstInstall() {
        PackageManager pm = getPackageManager();
        PackageInfo pi = null;
        try {
            pi = pm.getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (pi == null) {
            return false;
        }
        return pi.firstInstallTime == pi.lastUpdateTime;
    }
}
