package com.luxuan.musicapp;

public class MusicPlay {
    static{
        System.loadLibrary("avcodec-58");
        System.loadLibrary("avdevice-58");
        System.loadLibrary("avfilter-7");
        System.loadLibrary("avformat-58");
        System.loadLibrary("avutil-56");
        System.loadLibrary("postproc-55");
        System.loadLibrary("swresample-6");
        System.loadLibrary("swscale-5");
        System.loadLibrary("native-lib");
    }

    public native void play();

    public native void  stop();
}
