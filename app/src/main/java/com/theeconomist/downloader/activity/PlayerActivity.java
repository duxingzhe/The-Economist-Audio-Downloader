package com.theeconomist.downloader.activity;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.theeconomist.downloader.MusicService;
import com.theeconomist.downloader.R;
import com.theeconomist.downloader.bean.EventBusBean;
import com.theeconomist.downloader.bean.Mp3FileBean;
import com.theeconomist.downloader.bean.SeekBean;
import com.theeconomist.downloader.log.MyLog;
import com.theeconomist.downloader.utils.CommonUtil;
import com.theeconomist.downloader.utils.EventType;
import com.theeconomist.downloader.utils.FileUtil;
import com.ywl5320.bean.TimeBean;
import com.ywl5320.util.WlTimeUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import jp.wasabeef.glide.transformations.BlurTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class PlayerActivity extends BaseMusicActivity {

    @BindView(R.id.iv_point)
    ImageView ivPoint;
    @BindView(R.id.rl_cd)
    RelativeLayout rlCd;
    @BindView(R.id.tv_nowtime)
    TextView tvNowTime;
    @BindView(R.id.tv_totaltime)
    TextView tvTotalTime;
    @BindView(R.id.seek_bar)
    SeekBar seekBar;
    @BindView(R.id.iv_status)
    ImageView ivStatus;
    @BindView(R.id.iv_center)
    ImageView ivCenter;
    @BindView(R.id.iv_bg)
    ImageView ivBg;
    @BindView(R.id.pb_load)
    ProgressBar pbLoad;
    @BindView(R.id.tv_subtitle)
    TextView tvSubTitle;
    @BindView(R.id.tv_tip)
    TextView tvTip;

    private ValueAnimator cdAnimator;
    private ValueAnimator pointAnimator;
    private LinearInterpolator lin;
    private EventBusBean eventNextBean;
    private EventBusBean eventSeekBean;
    private SeekBean seekBean;

    private int position = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        setTitleTrans(R.color.color_trans);
        setBackView();
        setTitleLine(R.color.color_trans);
        setTitle(getPlayBean().getName());
        tvTip.setText("The Economist");
        tvSubTitle.setText(getPlayBean().getName());

        lin = new LinearInterpolator();
        initPointAnimate();
        initCDAnimate();
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("url", getPlayBean().getUrl());
        startService(intent);
        Glide.with(this).load(getPlayBean().getImgByte()).apply(RequestOptions.placeholderOf(R.mipmap.icon_cd_default_bg)).into(ivCenter);
        Glide.with(this).load(R.mipmap.icon_gray_bg)
                .apply(bitmapTransform(new BlurTransformation(25, 3)).placeholder(R.mipmap.icon_gray_bg))
                .into(ivBg);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                position = getTimeBean().getTotalSecs() * progress / 100;
                tvNowTime.setText(WlTimeUtil.secdsToDateFormat(position, getTimeBean().getTotalSecs()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(eventSeekBean == null) {
                    if(seekBean == null) {
                        seekBean = new SeekBean();
                    }
                    seekBean.setPosition(position);
                    seekBean.setSeekingfinished(false);
                    seekBean.setShowTime(false);

                    eventSeekBean = new EventBusBean(EventType.MUSIC_SEEK_TIME, seekBean);
                } else {
                    if(seekBean == null) {
                        seekBean = new SeekBean();
                    }
                    seekBean.setPosition(position);
                    seekBean.setSeekingfinished(false);
                    seekBean.setShowTime(false);

                    eventSeekBean.setType(EventType.MUSIC_SEEK_TIME);
                    eventSeekBean.setObject(seekBean);
                }
                EventBus.getDefault().post(eventSeekBean);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MyLog.d("position:" + position);
                if(eventSeekBean == null) {
                    if(seekBean == null) {
                        seekBean = new SeekBean();
                    }
                    seekBean.setPosition(position);
                    seekBean.setSeekingfinished(false);
                    seekBean.setShowTime(false);

                    eventSeekBean = new EventBusBean(EventType.MUSIC_SEEK_TIME, seekBean);
                } else {
                    if(seekBean == null) {
                        seekBean = new SeekBean();
                    }
                    seekBean.setPosition(position);
                    seekBean.setSeekingfinished(true);
                    seekBean.setShowTime(true);
                    eventSeekBean.setType(EventType.MUSIC_SEEK_TIME);
                    eventSeekBean.setObject(seekBean);
                }
                EventBus.getDefault().post(eventSeekBean);
            }
        });
        playRadio();
    }

    private void playRadio() {
        if(!TextUtils.isEmpty(getPlayBean().getUrl())) {
            if (!getPlayBean().getUrl().equals(playUrl)) {
                setCdRodio(0f);
                if (eventNextBean == null) {
                    eventNextBean = new EventBusBean(EventType.MUSIC_NEXT, getPlayBean().getUrl());
                } else {
                    eventNextBean.setType(EventType.MUSIC_NEXT);
                    eventNextBean.setObject(getPlayBean().getUrl());
                }
                EventBus.getDefault().post(eventNextBean);
                playUrl = getPlayBean().getUrl();
                getTimeBean().setTotalSecs(0);
                getTimeBean().setCurrSecs(0);
            }
        }
        initTime();
    }

    @Override
    public void onMusicStatus(int status) {
        super.onMusicStatus(status);
        switch (status) {
            case PLAY_STATUS_ERROR:
                if(ivPoint.getRotation() == 0f) {
                    startPointAnimat(0f, -40f);
                }
                ivStatus.setImageResource(R.drawable.play_selector);
                break;
            case PLAY_STATUS_LOADING:
                pbLoad.setVisibility(View.VISIBLE);
                ivStatus.setVisibility(View.GONE);
                if(ivPoint.getRotation() == -40f) {
                    rlCd.setRotation(getCdRodio());
                    startPointAnimat(-40f, 0f);
                }
                ivStatus.setImageResource(R.drawable.pause_selector);
                break;
            case PLAY_STATUS_UNLOADING:
                pbLoad.setVisibility(View.GONE);
                ivStatus.setVisibility(View.VISIBLE);
                break;
            case PLAY_STATUS_PLAYING:
                if(ivPoint.getRotation() == -40f) {
                    rlCd.setRotation(getCdRodio());
                    startPointAnimat(-40f, 0f);
                }
                ivStatus.setImageResource(R.drawable.pause_selector);
                break;
            case PLAY_STATUS_PAUSE:
                if(ivPoint.getRotation() == 0f) {
                    startPointAnimat(0f, -40f);
                }
                ivStatus.setImageResource(R.drawable.play_selector);
                break;
            case PLAY_STATUS_RESUME:
                break;
            case PLAY_STATUS_COMPLETE:
                if(ivPoint.getRotation() == 0f) {
                    startPointAnimat(0f, -40f);
                }
                ivStatus.setImageResource(R.drawable.play_selector);
                break;
            default:
                break;
        }
    }


    @Override
    public void timeInfo(TimeBean timeBean) {
        super.timeInfo(timeBean);
        updateTime(timeBean);
    }

    @Override
    public void onPlayHistoryChange() {
        super.onPlayHistoryChange();
        tvTip.setText("The Economist");
        setTitle(getPlayBean().getName());
        tvSubTitle.setText(getPlayBean().getName());
        initTime();
        updateTime(getTimeBean());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTime(getTimeBean());
        rlCd.setRotation(getCdRodio());
        ivPoint.setRotation(-40f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pointAnimator.cancel();
        cdAnimator.cancel();
        pointAnimator = null;
        cdAnimator = null;
    }

    @OnClick(R.id.iv_status)
    public void onClickStatus(View view) {
        if(musicStatus == PLAY_STATUS_PLAYING) {
            pauseMusic(true);
            ivStatus.setImageResource(R.drawable.play_selector);
        } else if(musicStatus == PLAY_STATUS_PAUSE) {
            pauseMusic(false);
            ivStatus.setImageResource(R.drawable.pause_selector);
            if(ivPoint.getRotation() == -40f) {
                startPointAnimat(-40f, 0f);
            }
        } else if(musicStatus == PLAY_STATUS_ERROR || musicStatus == PLAY_STATUS_COMPLETE) {
            playUrl = "";
            playRadio();
        }
    }

    @OnClick(R.id.iv_pre)
    public void onClickPre(View view) {
        playNext(false);
    }

    @OnClick(R.id.iv_next)
    public void onClickNext(View view) {
        playNext(true);
    }


    /**
     * 初始化指针动画
     */
    private void initPointAnimate() {
        ivPoint.setPivotX(CommonUtil.dip2px(PlayerActivity.this, 17));
        ivPoint.setPivotY(CommonUtil.dip2px(PlayerActivity.this, 15));
        pointAnimator = ValueAnimator.ofFloat(0, 0);
        pointAnimator.setTarget(ivPoint);
        pointAnimator.setRepeatCount(0);
        pointAnimator.setDuration(300);
        pointAnimator.setInterpolator(lin);
        pointAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float current = (Float) animation.getAnimatedValue();
                ivPoint.setRotation(current);
            }
        });

        pointAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                MyLog.d("onAnimationStart");
                if(!isPlaying()) {
                    pauseCDanimat();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                MyLog.d("onAnimationEnd");
                if(isPlaying()) {
                    resumeCDAnimate();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                MyLog.d("onAnimationCancel");
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                MyLog.d("onAnimationRepeat");
            }
        });

    }

    /**
     * 开始指针动画
     * @param from
     * @param end
     */
    private void startPointAnimat(float from, float end) {
        if(pointAnimator != null) {
            if(from < end) {
                if(!isPlaying()) {
                    return;
                }
            } else {
                if(isPlaying()) {
                    return;
                }
            }
            pointAnimator.setFloatValues(from, end);
            pointAnimator.start();
        }
    }

    /**
     * 初始化CD动画
     */
    private void initCDAnimate() {
        cdAnimator = ValueAnimator.ofFloat(rlCd.getRotation(), 360f + rlCd.getRotation());
        cdAnimator.setTarget(rlCd);
        cdAnimator.setRepeatCount(ValueAnimator.INFINITE);
        cdAnimator.setDuration(15000);
        cdAnimator.setInterpolator(lin);
        cdAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float current = (Float) animation.getAnimatedValue();
                setCdRodio(current);
                rlCd.setRotation(current);
            }
        });
    }

    /**
     * 开始cd动画
     */
    private void resumeCDAnimate() {
        if(cdAnimator != null && !cdAnimator.isRunning()) {
            cdAnimator.setFloatValues(rlCd.getRotation(), 360f + rlCd.getRotation());
            cdAnimator.start();
        }
    }

    /**
     * 暂停CD动画
     */
    private void pauseCDanimat() {
        if(cdAnimator != null && cdAnimator.isRunning()) {
            cdAnimator.cancel();
        }
    }

    private void updateTime(TimeBean timeBean) {
        if(timeBean != null) {
            if(timeBean.getTotalSecs() <= 0) {
                if(seekBar.getVisibility() == View.VISIBLE) {
                    seekBar.setVisibility(View.GONE);
                    tvTotalTime.setVisibility(View.GONE);
                }
                tvNowTime.setText(WlTimeUtil.secdsToDateFormat(timeBean.getCurrSecs(), timeBean.getTotalSecs()));
            } else {
                if(seekBar.getVisibility() == View.GONE) {
                    seekBar.setVisibility(View.VISIBLE);
                    tvTotalTime.setVisibility(View.VISIBLE);
                }
                tvTotalTime.setText(WlTimeUtil.secdsToDateFormat(timeBean.getTotalSecs(), timeBean.getTotalSecs()));
                tvNowTime.setText(WlTimeUtil.secdsToDateFormat(timeBean.getCurrSecs(), timeBean.getTotalSecs()));
                seekBar.setProgress(getProgress());
            }
        }
    }

    private void initTime() {
        if(getTimeBean().getTotalSecs() > 0) {
            seekBar.setVisibility(View.VISIBLE);
            tvTotalTime.setVisibility(View.VISIBLE);
            seekBar.setProgress(getProgress());
        } else {
            seekBar.setVisibility(View.GONE);
            tvTotalTime.setVisibility(View.GONE);
        }
    }

    private void playNext(boolean next) {
        if(FileUtil.fileList != null && FileUtil.fileList.size() > 0) {
            int size = FileUtil.fileList.size();
            for(int i = 0; i < size; i++) {
                Mp3FileBean mp3File = FileUtil.fileList.get(i);
                //当前播放的节目
                if(mp3File.index == getPlayBean().getIndex()){
                    if(next) {
                        if(i == size - 1) {
                            showToast("已经是最新节目了");
                        } else if(i < size - 1) {
                            mp3File = FileUtil.fileList.get(i+1);
                            getPlayBean().setName(mp3File.name);
                            getPlayBean().setUrl(mp3File.path);
                            getPlayBean().setIndex(mp3File.index);
                            playRadio();
                            onPlayHistoryChange();
                        }
                        break;
                    } else {
                        if(i == 0) {
                            showToast("已经没有节目了");
                        } else if(i > 0) {
                            mp3File = FileUtil.fileList.get(i-1);
                            getPlayBean().setName(mp3File.name);
                            getPlayBean().setUrl(mp3File.path);
                            getPlayBean().setIndex(mp3File.index);
                            playRadio();
                            onPlayHistoryChange();
                        }
                        break;
                    }
                }
            }
        } else {
            showToast("没有历史节目");
        }
    }
}
