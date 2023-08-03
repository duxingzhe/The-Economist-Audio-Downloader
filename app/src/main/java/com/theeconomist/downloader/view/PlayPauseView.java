package com.theeconomist.downloader.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.theeconomist.downloader.R;

public class PlayPauseView extends View{

    // 中心点X轴坐标
    private int viewCenterX;
    // 中心点Y轴坐标
    private int viewCenterY;
    // 有效长度的一般（View长宽较小者的一半）
    private int viewHalfLength;
    //View宽度
    private int mWidth;
    //View高度
    private int mHeight;
    private Paint mPaint;
    //暂停时左侧竖条Path
    private Path mLeftPath;
    //暂停时右侧竖条Path
    private Path mRightPath;
    private Path mTrianglePath;
    //两个暂停竖条中间的空隙,默认为两侧竖条的宽度
    private float mGapWidth;

    // 进度
    private int mPlayingProgress;
    private Rect mRect;
    private boolean isPlaying;
    //圆内矩形宽度
    private float mRectWidth;
    //圆内矩形高度
    private float mRectHeight;
    private float mTriangleLength;
    // 矩形左侧上侧坐标
    private float mRectLT;
    // 三角形左上角坐标
    private float mTriangleTop;
    private float mTriangleLeft;
    // 圆的半径
    private float mRadius;
    private int mBgColor = Color.WHITE;
    private int mBtnColor = Color.BLACK;
    // 包围进度圆弧的矩形
    private RectF rectCircle=new RectF();
    private RectF rectProgress = new RectF();
    private float mPadding;

    public PlayPauseView(Context context) {
        super(context);
    }

    public PlayPauseView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PlayPauseView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mLeftPath = new Path();
        mRightPath = new Path();
        mTrianglePath=new Path();
        mRect = new Rect();
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PlayPauseView);
        mBgColor = ta.getColor(R.styleable.PlayPauseView_bg_color, Color.WHITE);
        mBtnColor = ta.getColor(R.styleable.PlayPauseView_btn_color, getResources().getColor(R.color.primary));
        mGapWidth = ta.getDimensionPixelSize(R.styleable.PlayPauseView_gap_width, dp2px(context, 0));
        mPadding = ta.getDimensionPixelSize(R.styleable.PlayPauseView_space_padding, dp2px(context, 0));
        ta.recycle();

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY) {
            mWidth = Math.min(mWidth, mHeight);
        } else {
            mWidth = dp2px(getContext(), 50);
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            mHeight = Math.min(mWidth, mHeight);
        } else {
            mHeight = dp2px(getContext(), 50);
        }
        mWidth = mHeight = Math.min(mWidth, mHeight);
        setMeasuredDimension(mWidth, mHeight);

        int viewHeight = getMeasuredHeight();
        int viewWidth = getMeasuredWidth();
        viewCenterX = viewWidth / 2;
        viewCenterY = viewHeight / 2;
        viewHalfLength = viewHeight < viewWidth ? viewHeight / 2 : viewWidth / 2;

        int paintCircle = viewHalfLength / 15;
        rectCircle.left = viewCenterX - (viewHalfLength - paintCircle / 2f);
        rectCircle.top = viewCenterY - (viewHalfLength - paintCircle / 2f);
        rectCircle.right = viewCenterX + (viewHalfLength - paintCircle / 2f);
        rectCircle.bottom = viewCenterY + (viewHalfLength - paintCircle / 2f);

        int paintProgressWidth = viewHalfLength / 8;
        rectProgress.left = viewCenterX - (viewHalfLength - paintProgressWidth / 2f);
        rectProgress.top = viewCenterY - (viewHalfLength - paintProgressWidth / 2f);
        rectProgress.right = viewCenterX + (viewHalfLength - paintProgressWidth / 2f);
        rectProgress.bottom = viewCenterY + (viewHalfLength - paintProgressWidth / 2f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = mHeight = w;
        initValue();
    }

    private void initValue() {

        mRadius = mWidth / 2f;
        mPadding =  mRadius / 3f;

        //矩形宽高的一半
        float space = (float) (mRadius / Math.sqrt(2) - mPadding);
        mRectLT = mRadius - space;
        mTriangleLeft=mRadius - 0.7f * space;
        mTriangleTop=mRadius - 1.5f * (float) Math.sin(Math.sqrt(3)/2) * space;

        float rectRB = mRadius + space;
        mRect.top = (int) mRectLT;
        mRect.bottom = (int) rectRB;
        mRect.left = (int) mRectLT;
        mRect.right = (int) rectRB;

        mRectWidth = 2 * space;
        mRectHeight = 2 * space;
        mTriangleLength= 2.5f * space;
        mGapWidth = mRectWidth / 3;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(isPlaying()) {
            mLeftPath.rewind();
            mRightPath.rewind();

            //暂停时左右两边矩形距离
            float distance = mGapWidth;
            //一个矩形的宽度
            float barWidth = mRectWidth / 2 - distance / 2;
            //右边矩形左上角
            float rightLeftTop = barWidth + distance;

            mPaint.setColor(mBtnColor);
            mPaint.setStyle(Paint.Style.FILL);

            mLeftPath.moveTo(mRectLT, mRectLT);
            mLeftPath.lineTo(mRectLT, mRectHeight + mRectLT);
            mLeftPath.lineTo(barWidth + mRectLT, mRectHeight + mRectLT);
            mLeftPath.lineTo(barWidth + mRectLT, mRectLT);
            mLeftPath.close();

            mRightPath.moveTo(rightLeftTop + mRectLT , mRectLT);
            mRightPath.lineTo(rightLeftTop + mRectLT, mRectHeight + mRectLT);
            mRightPath.lineTo(rightLeftTop + mRectLT + barWidth, mRectHeight + mRectLT);
            mRightPath.lineTo(rightLeftTop + mRectLT + barWidth, mRectLT);

            mRightPath.close();
            canvas.save();

            canvas.drawPath(mLeftPath, mPaint);
            canvas.drawPath(mRightPath, mPaint);
        }else{
            //三角形
            mPaint.setColor(mBtnColor);
            mPaint.setStyle(Paint.Style.FILL);

            mTrianglePath.moveTo(mTriangleLeft, mTriangleTop);
            mTrianglePath.lineTo(mTriangleLeft, mTriangleLength + mTriangleTop);
            mTrianglePath.lineTo(mTriangleLeft+mTriangleLength*(float)Math.sin(Math.sqrt(3)/2),
                    mTriangleTop + mTriangleLength/2);
            mTrianglePath.close();
            canvas.save();

            canvas.drawPath(mTrianglePath, mPaint);
        }

        mPaint.setStrokeWidth(viewHalfLength/15f);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(rectCircle, 0, 360, false, mPaint);
        mPaint.setStrokeWidth(viewHalfLength / 8f);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(rectProgress, -90, mPlayingProgress * 3.6f, false, mPaint);
        canvas.restore();

    }

    public void play() {
        setPlaying(true);
        invalidate();
    }

    public void pause() {
        setPlaying(false);
        invalidate();
    }

    public int dp2px(Context context, float dpVal) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (density * dpVal + 0.5f);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    /** 设置进度 0-100区间 */
    public void setProgress(int progress) {

        if (progress < 0) {
            progress = 0;
        }
        if (progress > 100) {
            progress = 100;
        }
        mPlayingProgress = progress;
        invalidate();
    }

}
