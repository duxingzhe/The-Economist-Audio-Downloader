package com.theeconomist.downloader;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import com.theeconomist.downloader.utils.CoverLoader;

public class PlayerApplication extends Application {

    private Handler globalHandler;

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        globalHandler=new Handler();
        context=this;

        CoverLoader.get().init(context);
    }

    @Override
    public void onLowMemory() {
        System.gc();
        super.onLowMemory();
    }

    public Handler getGlobalHandler(){
        return globalHandler;
    }

    public static Context getInstance(){
        return context;
    }
}
