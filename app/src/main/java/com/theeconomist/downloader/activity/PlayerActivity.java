package com.theeconomist.downloader.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.theeconomist.downloader.MusicService;
import com.theeconomist.downloader.R;
import com.theeconomist.downloader.bean.EventBusBean;
import com.theeconomist.downloader.bean.SeekBean;
import com.theeconomist.downloader.bean.TimeBean;
import com.theeconomist.downloader.databinding.ActivityPlayBinding;
import com.theeconomist.downloader.log.MyLog;
import com.theeconomist.downloader.utils.CoverLoader;
import com.theeconomist.downloader.utils.EventType;
import com.ywl5320.wlmedia.util.WlTimeUtil;

import org.greenrobot.eventbus.EventBus;

import jp.wasabeef.glide.transformations.BlurTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class PlayerActivity extends BaseMusicActivity {

    private ActivityPlayBinding viewBinding;

    private EventBusBean eventNextBean;
    private EventBusBean eventSeekBean;
    private SeekBean seekBean;

    private int position = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        viewBinding=DataBindingUtil.setContentView(this, R.layout.activity_play);
        setTitleTrans(R.color.color_trans);
        setBackView();
        setTitleLine(R.color.color_trans);
        setTitle(getPlayBean().getName());
        if(TextUtils.isEmpty(getPlayBean().getAlbumName())) {
            viewBinding.tvTip.setText("The Economist");
        }else{
            viewBinding.tvTip.setText("The Economist - " + getPlayBean().getAlbumName());
        }
        viewBinding.tvSubtitle.setText(getPlayBean().getName());

        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("url", getPlayBean().getUrl());
        startService(intent);
        viewBinding.albumCoverView.setCoverBitmap(CoverLoader.get().loadBitmapFromByteArray(getPlayBean().getImgByte()));
        viewBinding.albumCoverView.initNeedle(false);
        Glide.with(this).load(R.mipmap.icon_gray_bg)
                .apply(bitmapTransform(new BlurTransformation(25, 3)).placeholder(R.mipmap.icon_gray_bg))
                .into(viewBinding.ivBg);

        viewBinding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                position = getTimeBean().getTotalSecs() * progress / 100;
                viewBinding.tvNowtime.setText(WlTimeUtil.secondToTimeFormat(getTimeBean().getTotalSecs()));
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

        viewBinding.ivStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(musicStatus == PLAY_STATUS_PLAYING) {
                    pauseMusic(true);
                    viewBinding.ivStatus.setImageResource(R.drawable.play_selector);
                } else if(musicStatus == PLAY_STATUS_PAUSE) {
                    pauseMusic(false);
                    viewBinding.ivStatus.setImageResource(R.drawable.pause_selector);

                } else if(musicStatus == PLAY_STATUS_ERROR || musicStatus == PLAY_STATUS_COMPLETE) {
                    playUrl = "";
                    playMusic();
                }
            }
        });

        viewBinding.ivPre.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                playNext(false);
            }
        });

        viewBinding.ivNext.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                playNext(true);
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
                viewBinding.albumCoverView.pause();
                viewBinding.ivStatus.setImageResource(R.drawable.play_selector);
                break;
            case PLAY_STATUS_LOADING:
                viewBinding.pbLoad.setVisibility(View.VISIBLE);
                viewBinding.ivStatus.setVisibility(View.GONE);
                viewBinding.albumCoverView.pause();
                viewBinding.ivStatus.setImageResource(R.drawable.pause_selector);
                break;
            case PLAY_STATUS_UNLOADING:
                viewBinding.pbLoad.setVisibility(View.GONE);
                viewBinding.ivStatus.setVisibility(View.VISIBLE);
                break;
            case PLAY_STATUS_PLAYING:
                viewBinding.albumCoverView.play();
                viewBinding.ivStatus.setImageResource(R.drawable.pause_selector);
                break;
            case PLAY_STATUS_PAUSE:
                viewBinding.albumCoverView.pause();
                viewBinding.ivStatus.setImageResource(R.drawable.play_selector);
                break;
            case PLAY_STATUS_RESUME:
                break;
            case PLAY_STATUS_COMPLETE:
                viewBinding.albumCoverView.pause();
                viewBinding.ivStatus.setImageResource(R.drawable.pause_selector);
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
            viewBinding.tvTip.setText("The Economist");
        }else{
            viewBinding.tvTip.setText("The Economist - " + getPlayBean().getAlbumName());
        }
        setTitle(getPlayBean().getName());
        viewBinding.tvSubtitle.setText(getPlayBean().getName());
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

    private void updateTime(TimeBean timeBean) {
        if(timeBean != null) {
            if(timeBean.getTotalSecs() <= 0) {
                if(viewBinding.seekBar.getVisibility() == View.VISIBLE) {
                    viewBinding.seekBar.setVisibility(View.GONE);
                    viewBinding.tvTotaltime.setVisibility(View.GONE);
                }
                viewBinding.tvNowtime.setText(WlTimeUtil.secondToTimeFormat(timeBean.getTotalSecs()));
            } else {
                if(viewBinding.seekBar.getVisibility() == View.GONE) {
                    viewBinding.seekBar.setVisibility(View.VISIBLE);
                    viewBinding.tvTotaltime.setVisibility(View.VISIBLE);
                }
                viewBinding.tvTotaltime.setText(WlTimeUtil.secondToTimeFormat(timeBean.getTotalSecs()));
                viewBinding.tvNowtime.setText(WlTimeUtil.secondToTimeFormat(timeBean.getTotalSecs()));
                viewBinding.seekBar.setProgress(getProgress());
            }
        }
    }

    private void initTime() {
        if(getTimeBean().getTotalSecs() > 0) {
            viewBinding.seekBar.setVisibility(View.VISIBLE);
            viewBinding.tvTotaltime.setVisibility(View.VISIBLE);
            viewBinding.seekBar.setProgress(getProgress());
        } else {
            viewBinding.seekBar.setVisibility(View.GONE);
            viewBinding.tvTotaltime.setVisibility(View.GONE);
        }
    }

}
