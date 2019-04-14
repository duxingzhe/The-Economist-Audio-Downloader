package com.luxuan.musicapp.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * 音乐频谱
 */
public class EqualizerView extends View {

    private final int DN_W = 470;//view宽度与单个音频块占比 - 正常480 需微调
    private final int DN_H = 360;//view高度与单个音频块占比
    private final int DN_SL = 15;//单个音频块宽度
    private final int DN_SW = 5;//单个音频块高度

    private int hgap = 0;
    private int vgap = 0;
    private int levelStep = 0;
    private float strokeWidth = 0;
    private float strokeLength = 0;

    protected final static int MAX_LEVEL = 30;//音量柱·音频块 - 最大个数

    protected final static int CYLINDER_NUM = 26;//音量柱 - 最大个数

    protected Paint mPaint = null;//画笔

    protected byte[] mData = new byte[CYLINDER_NUM];//音量柱 数组

    boolean mDataEn = true;

    //构造函数初始化画笔
    public EqualizerView(Context context) {
        super(context);

        mPaint = new Paint();//初始化画笔工具
        mPaint.setAntiAlias(true);//抗锯齿
        mPaint.setColor(Color.RED);//画笔颜色

        mPaint.setStrokeJoin(Paint.Join.ROUND); //频块圆角
        mPaint.setStrokeCap(Paint.Cap.ROUND); //频块圆角

        levelStep = 230 / MAX_LEVEL;
    }

    //执行 Layout 操作
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        float w, h, xr, yr;

        w = right - left;
        h = bottom - top;
        xr = w / (float) DN_W;
        yr = h / (float) DN_H;

        strokeWidth = DN_SW * yr;
        strokeLength = DN_SL * xr;
        hgap = (int) ((w - strokeLength * CYLINDER_NUM) / (CYLINDER_NUM + 1));
        vgap = (int) (h / (MAX_LEVEL + 2));//频谱块高度

        mPaint.setStrokeWidth(strokeWidth); //设置频谱块宽度
    }

    //绘制频谱块和倒影
    protected void drawCylinder(Canvas canvas, float x, byte value) {
        if (value == 0) {
            value = 1;
        }//最少有一个频谱块
        for (int i = 0; i < value; i++) { //每个能量柱绘制value个能量块
            float y = (getHeight() / 2 - i * vgap - vgap);//计算y轴坐标
            float y1 = (getHeight() / 2 + i * vgap + vgap);
            //绘制频谱块
            mPaint.setColor(Color.RED);//画笔颜色
            canvas.drawLine(x, y, (x + strokeLength), y, mPaint);//绘制频谱块

            //绘制音量柱倒影
            if (i <= 6 && value > 0) {
                mPaint.setColor(Color.RED);//画笔颜色
                mPaint.setAlpha(100 - (100 / 6 * i));//倒影颜色
                canvas.drawLine(x, y1, (x + strokeLength), y1, mPaint);//绘制频谱块
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);

        int j = -4;
        for (int i = 0; i < CYLINDER_NUM / 2 - 4; i++) { //绘制25个能量柱

            drawCylinder(canvas, strokeWidth / 2 + hgap + i * (hgap + strokeLength), mData[i]);
        }
        for (int i = CYLINDER_NUM; i >= CYLINDER_NUM / 2 - 4; i--) {
            j++;
            drawCylinder(canvas, strokeWidth / 2 + hgap + (CYLINDER_NUM / 2 + j - 1) * (hgap + strokeLength), mData[i - 1]);
        }
    }

    /**
     * @param fft
     */
    public void updateVisualizer(byte[] fft) {
        byte[] model = new byte[fft.length / 2 + 1];
        if (mDataEn) {
            model[0] = (byte) Math.abs(fft[1]);
            int j = 1;
            for (int i = 2; i < fft.length; ) {
                model[j] = (byte) Math.hypot(fft[i], fft[i + 1]);
                i += 2;
                j++;
            }
        } else {
            for (int i = 0; i < CYLINDER_NUM; i++) {
                model[i] = 0;
            }
        }
        for (int i = 0; i < CYLINDER_NUM; i++) {
            final byte a = (byte) (Math.abs(model[CYLINDER_NUM - i]) / levelStep);

            final byte b = mData[i];
            if (a > b) {
                mData[i] = a;
            } else {
                if (b > 0) {
                    mData[i]--;
                }
            }
        }
        invalidate();
    }
}
