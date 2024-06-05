package com.cadnative.firevisioniptv;

import android.app.Application;
import android.content.Context;

public class FirevisionApplication extends Application {
    private static FirevisionApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}
