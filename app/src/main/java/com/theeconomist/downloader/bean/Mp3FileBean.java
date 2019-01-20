package com.theeconomist.downloader.bean;

import android.graphics.Bitmap;

public class Mp3FileBean {

    public Mp3FileBean(String path){
        this.path=path;
    }

    public String path;

    public long duration;

    public String name;

    public long fileSize;

    public byte[] coverImg;
}
