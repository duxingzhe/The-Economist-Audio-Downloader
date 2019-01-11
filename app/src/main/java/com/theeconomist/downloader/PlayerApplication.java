package com.theeconomist.downloader;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;

public class PlayerApplication extends Application {

    private Handler globalHandler;

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        globalHandler=new Handler();
        context=this;
    }
    @Override
    public void onTerminate() {

        super.onTerminate();
    }
    @Override
    public void onLowMemory() {
        System.gc();
        super.onLowMemory();
    }
    @Override
    public void onTrimMemory(int level) {

        super.onTrimMemory(level);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
    }

    public Handler getGlobalHandler(){
        return globalHandler;
    }

    public static Context getInstance(){
        return context;
    }
}
