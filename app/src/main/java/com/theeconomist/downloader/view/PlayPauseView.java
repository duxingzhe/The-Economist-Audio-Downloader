package com.theeconomist.downloader.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

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
    //两个暂停竖条中间的空隙,默认为两侧竖条的宽度
    private float mGapWidth;
    //动画Progress
    private float mAnimationProgress;
    /** 进度 */
    private int mPlayingProgress;
    private Rect mRect;
    private boolean isPlaying;
    //圆内矩形宽度
    private float mRectWidth;
    //圆内矩形高度
    private float mRectHeight;
    //矩形左侧上侧坐标
    private float mRectLT;
    //圆的半径
    private float mRadius;
    private int mBgColor = Color.WHITE;
    private int mBtnColor = Color.BLACK;
    /** 包围进度圆弧的矩形 */
    private RectF rectCircle=new RectF();
    private RectF rectProgress = new RectF();
    private float mPadding;
    //动画时间
    private int mAnimDuration = 200;
    private boolean isFirstChange=true;

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
        mRect = new Rect();
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PlayPauseView);
        mBgColor = ta.getColor(R.styleable.PlayPauseView_bg_color, Color.WHITE);
        mBtnColor = ta.getColor(R.styleable.PlayPauseView_btn_color, getResources().getColor(R.color.color_ec4c48));
        mGapWidth = ta.getDimensionPixelSize(R.styleable.PlayPauseView_gap_width, dp2px(context, 0));
        mPadding = ta.getDimensionPixelSize(R.styleable.PlayPauseView_space_padding, dp2px(context, 0));
        mAnimDuration = ta.getInt(R.styleable.PlayPauseView_anim_duration, 200);
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
        rectCircle.left = viewCenterX - (viewHalfLength - paintCircle / 2);
        rectCircle.top = viewCenterY - (viewHalfLength - paintCircle / 2);
        rectCircle.right = viewCenterX + (viewHalfLength - paintCircle / 2);
        rectCircle.bottom = viewCenterY + (viewHalfLength - paintCircle / 2);

        int paintProgressWidth = viewHalfLength / 8;
        rectProgress.left = viewCenterX - (viewHalfLength - paintProgressWidth / 2);
        rectProgress.top = viewCenterY - (viewHalfLength - paintProgressWidth / 2);
        rectProgress.right = viewCenterX + (viewHalfLength - paintProgressWidth / 2);
        rectProgress.bottom = viewCenterY + (viewHalfLength - paintProgressWidth / 2);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = mHeight = w;
        initValue();
    }

    private void initValue() {

        mRadius = mWidth / 2;
        mPadding =  mRadius / 3f;
        float space = (float) (mRadius / Math.sqrt(2) - mPadding); //矩形宽高的一半
        mRectLT = mRadius - space;
        float rectRB = mRadius + space;
        mRect.top = (int) mRectLT;
        mRect.bottom = (int) rectRB;
        mRect.left = (int) mRectLT;
        mRect.right = (int) rectRB;

        mRectWidth = 2 * space;
        mRectHeight = 2 * space;
        mGapWidth = mRectWidth / 3;
        mAnimationProgress = isPlaying ? 0 : 1;
        mAnimDuration = 200;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mLeftPath.rewind();
        mRightPath.rewind();

        mPaint.setColor(mBgColor);
        canvas.drawCircle(mWidth / 2, mHeight / 2, mRadius, mPaint);
        //暂停时左右两边矩形距离
        float distance = mGapWidth * (1 - mAnimationProgress);
        //一个矩形的宽度
        float barWidth = mRectWidth / 2 - distance / 2;
        //左边矩形左上角
        float leftLeftTop = barWidth * mAnimationProgress;
        //右边矩形左上角
        float rightLeftTop = barWidth + distance;
        //右边矩形右上角
        float rightRightTop = 2 * barWidth + distance;
        //右边矩形右下角
        float rightRightBottom = rightRightTop - barWidth * mAnimationProgress;

        mPaint.setColor(mBtnColor);
        mPaint.setStyle(Paint.Style.FILL);

        mLeftPath.moveTo(leftLeftTop + mRectLT, mRectLT);
        mLeftPath.lineTo(mRectLT, mRectHeight + mRectLT);
        mLeftPath.lineTo(barWidth + mRectLT, mRectHeight + mRectLT);
        mLeftPath.lineTo(barWidth + mRectLT, mRectLT);
        mLeftPath.close();

        mRightPath.moveTo(rightLeftTop + mRectLT, mRectLT);
        mRightPath.lineTo(rightLeftTop + mRectLT, mRectHeight + mRectLT);
        mRightPath.lineTo(rightLeftTop + mRectLT + barWidth, mRectHeight + mRectLT);
        mRightPath.lineTo(rightRightBottom + mRectLT, mRectLT);
        mRightPath.close();
        canvas.save();

        canvas.translate(mRectHeight / 8f * mAnimationProgress, 0);
        float progress = isPlaying ? (1 - mAnimationProgress) : mAnimationProgress;
        int corner = 90;
        float rotation = isPlaying ? corner * (1 + progress) : corner * progress;
        canvas.rotate(rotation, mWidth / 2f, mHeight / 2f);
        canvas.drawPath(mLeftPath, mPaint);
        canvas.drawPath(mRightPath, mPaint);

        mPaint.setStrokeWidth(viewHalfLength/15);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(rectCircle, 0, 360, false, mPaint);
        mPaint.setStrokeWidth(viewHalfLength / 8);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(rectProgress, -90, mPlayingProgress * 3.6f, false, mPaint);
        canvas.restore();

    }

    public ValueAnimator getPlayPauseAnim() {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(isPlaying ? 1 : 0, isPlaying ? 0 : 1);
        valueAnimator.setDuration(mAnimDuration);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimationProgress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        return valueAnimator;
    }

    public void play() {
        if(isFirstChange) {
            isFirstChange=false;
            if (getPlayPauseAnim() != null) {
                getPlayPauseAnim().cancel();
            }
            setPlaying(true);
            getPlayPauseAnim().start();
        }
    }

    public void pause() {
        if (getPlayPauseAnim() != null) {
            getPlayPauseAnim().cancel();
        }
        setPlaying(false);
        getPlayPauseAnim().start();
        isFirstChange=true;
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
