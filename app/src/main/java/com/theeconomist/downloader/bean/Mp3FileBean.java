package com.theeconomist.downloader.bean;

public class Mp3FileBean {

    public Mp3FileBean(String path){
        this.path=path;
    }

    public String path;

    public long duration;

    public long modifiedTime;

    public long fileSize;
}
