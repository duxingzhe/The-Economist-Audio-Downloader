package com.theeconomist.downloader.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.theeconomist.downloader.R;
import com.theeconomist.downloader.bean.Mp3FileBean;

/**
 * 专辑封面图片加载器
 * Created by wcy on 2015/11/27.
 */
public class CoverLoader {

    private Context context;
    private int roundLength;

    public static CoverLoader get() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        private static CoverLoader instance = new CoverLoader();
    }

    private CoverLoader() {
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        roundLength = CommonUtil.getScreenWidth(context) / 2;
    }

    private Bitmap loadCoverFromFile(Mp3FileBean music) {
        Bitmap bitmap;
        bitmap = loadBitmapFromByteArray(music.coverImg);
        bitmap = ImageUtil.resizeImage(bitmap, roundLength, roundLength);
        return ImageUtil.createCircleImage(bitmap);
    }

    public Bitmap loadDefaultCover(){
        return BitmapFactory.decodeResource(context.getResources(), R.mipmap.icon_mini_default_bg);
    }

    public Bitmap loadBitmapFromByteArray(byte[] bitArray) {
        if (bitArray.length != 0) {
            Bitmap bitmap= BitmapFactory.decodeByteArray(bitArray, 0, bitArray.length);
            bitmap = ImageUtil.resizeImage(bitmap, roundLength, roundLength);
            return ImageUtil.createCircleImage(bitmap);
        } else {
            return null;
        }
    }

    public void setRoundLength(int roundLength) {
        if (this.roundLength != roundLength) {
            this.roundLength = roundLength;
        }
    }
}
