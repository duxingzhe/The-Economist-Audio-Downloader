package com.theeconomist.downloader.bean;

/**
 * Created by ywl on 2018-1-13.
 */

public class PlayBean extends BaseBean{

    private String name;
    private String url;
    private byte[] imgByte;
    private int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public byte[] getImgByte() {
        return imgByte;
    }

    public void setImg(byte[] imgByte) {
        this.imgByte = imgByte;
    }
}
