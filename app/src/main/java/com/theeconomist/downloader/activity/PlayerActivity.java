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
import com.theeconomist.downloader.bean.TimeBean;
import com.theeconomist.downloader.log.MyLog;
import com.theeconomist.downloader.utils.CommonUtil;
import com.theeconomist.downloader.utils.CoverLoader;
import com.theeconomist.downloader.utils.EventType;
import com.theeconomist.downloader.utils.FileUtil;
import com.theeconomist.downloader.view.AlbumCoverView;
import com.ywl5320.wlmedia.util.WlTimeUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import jp.wasabeef.glide.transformations.BlurTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class PlayerActivity extends BaseMusicActivity {

    @BindView(R.id.tv_nowtime)
    TextView tvNowTime;
    @BindView(R.id.tv_totaltime)
    TextView tvTotalTime;
    @BindView(R.id.seek_bar)
    SeekBar seekBar;
    @BindView(R.id.iv_status)
    ImageView ivStatus;
    @BindView(R.id.iv_bg)
    ImageView ivBg;
    @BindView(R.id.pb_load)
    ProgressBar pbLoad;
    @BindView(R.id.tv_subtitle)
    TextView tvSubTitle;
    @BindView(R.id.tv_tip)
    TextView tvTip;
    private AlbumCoverView mAlbumCoverView;

    private ValueAnimator cdAnimator;
    private ValueAnimator pointAnimator;
    private EventBusBean eventNextBean;
    private EventBusBean eventSeekBean;
    private SeekBean seekBean;

    private int position = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        mAlbumCoverView=(AlbumCoverView) findViewById(R.id.album_cover_view);
        setTitleTrans(R.color.color_trans);
        setBackView();
        setTitleLine(R.color.color_trans);
        setTitle(getPlayBean().getName());
        if(TextUtils.isEmpty(getPlayBean().getAlbumName())) {
            tvTip.setText("The Economist");
        }else{
            tvTip.setText("The Economist - " + getPlayBean().getAlbumName());
        }
        tvSubTitle.setText(getPlayBean().getName());

        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("url", getPlayBean().getUrl());
        startService(intent);
        mAlbumCoverView.setCoverBitmap(CoverLoader.get().loadBitmapFromByteArray(getPlayBean().getImgByte()));
        mAlbumCoverView.initNeedle(false);
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
        playMusic();
    }

    @Override
    public void playMusic() {
        if(!TextUtils.isEmpty(getPlayBean().getUrl())) {
            if (!getPlayBean().getUrl().equals(playUrl)) {
                setCdRadio(0f);
                if (eventNextBean == null) {
                    eventNextBean = new EventBusBean(EventType.MUSIC_NEXT, getPlayBean().getUrl());
                } else {
                    eventNextBean.setType(EventType.MUSIC_NEXT);
                    eventNextBean.setObject(getPlayBean().getUrl());
                }
                EventBus.getDefault().post(eventNextBean);
                playUrl = getPlayBean().getUrl();
                getTimeBean().setTotalSecs(getPlayBean().getDuration());
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
                mAlbumCoverView.pause();
                ivStatus.setImageResource(R.drawable.play_selector);
                break;
            case PLAY_STATUS_LOADING:
                pbLoad.setVisibility(View.VISIBLE);
                ivStatus.setVisibility(View.GONE);
                mAlbumCoverView.pause();
                ivStatus.setImageResource(R.drawable.pause_selector);
                break;
            case PLAY_STATUS_UNLOADING:
                pbLoad.setVisibility(View.GONE);
                ivStatus.setVisibility(View.VISIBLE);
                break;
            case PLAY_STATUS_PLAYING:
                mAlbumCoverView.play();
                ivStatus.setImageResource(R.drawable.pause_selector);
                break;
            case PLAY_STATUS_PAUSE:
                mAlbumCoverView.pause();
                ivStatus.setImageResource(R.drawable.play_selector);
                break;
            case PLAY_STATUS_RESUME:
                break;
            case PLAY_STATUS_COMPLETE:
                mAlbumCoverView.pause();
                ivStatus.setImageResource(R.drawable.pause_selector);
                playNextMusic();
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
        if(TextUtils.isEmpty(getPlayBean().getAlbumName())) {
            tvTip.setText("The Economist");
        }else{
            tvTip.setText("The Economist - " + getPlayBean().getAlbumName());
        }
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

        } else if(musicStatus == PLAY_STATUS_ERROR || musicStatus == PLAY_STATUS_COMPLETE) {
            playUrl = "";
            playMusic();
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

}
