package com.theeconomist.downloader.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.theeconomist.downloader.R;
import com.theeconomist.downloader.bean.EventBusBean;
import com.theeconomist.downloader.bean.Mp3FileBean;
import com.theeconomist.downloader.bean.PlayBean;
import com.theeconomist.downloader.bean.TimeBean;
import com.theeconomist.downloader.log.MyLog;
import com.theeconomist.downloader.utils.EventType;
import com.theeconomist.downloader.utils.FileUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Optional;

/**
 * Created by ywl on 2018/1/12.
 */

public abstract class BaseMusicActivity extends BaseActivity{

    @Nullable
    @BindView(R.id.iv_mini_bg)
    ImageView ivMiniBg;
    
    @Nullable
    @BindView(R.id.tv_mini_name)
    TextView tvMiniName;

    @Nullable
    @BindView(R.id.tv_mini_subname)
    TextView tvMiniSubName;

    @Nullable
    @BindView(R.id.iv_mini_playstatus)
    ImageView ivMiniPlayStatus;

    @Nullable
    @BindView(R.id.rl_mini_bar)
    RelativeLayout rlMiniBar;

    //暂停、播放状态
    private static EventBusBean eventPauseResumeBean;
    private static float cdRadio = 0f;
    private static PlayBean playBean;
    private static TimeBean timeBean;
    private static boolean isPlaying = false;

    //当前播放url
    public static String playUrl = "";

    public static int musicStatus = -1;

    public static final int PLAY_STATUS_ERROR = 0;
    public static final int PLAY_STATUS_LOADING = 1;
    public static final int PLAY_STATUS_UNLOADING = 2;
    public static final int PLAY_STATUS_PLAYING = 3;
    public static final int PLAY_STATUS_PAUSE = 4;
    public static final int PLAY_STATUS_RESUME = 5;
    public static final int PLAY_STATUS_COMPLETE = 6;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(ivMiniBg != null) {
            Glide.with(this).load(getPlayBean().getImgByte()).apply(RequestOptions.errorOf(R.mipmap.file_mp3_icon)).into(ivMiniBg);
        }
        if(tvMiniName != null) {
            if(!tvMiniName.getText().toString().trim().equals(getPlayBean().getName())) {
                tvMiniName.setText(getPlayBean().getName());
            }
        }
        if(tvMiniSubName != null) {
            if(TextUtils.isEmpty(getPlayBean().getAlbumName())) {
                tvMiniSubName.setText("The Economist");
            }else{
                tvMiniSubName.setText("The Economist - "+getPlayBean().getAlbumName());
            }
        }
        onMusicStatus(musicStatus);
    }

    @Optional
    @OnClick(R.id.rl_mini_bar)
    public void onClickLive(View view) {
        if(playBean != null ) {
            startActivity(this, PlayerActivity.class);
        }
    }

    @Optional
    @OnClick(R.id.iv_mini_playstatus)
    public void onClickPlayStatus(View view) {
        if(musicStatus == PLAY_STATUS_PLAYING) {
            pauseMusic(true);
            if(ivMiniPlayStatus != null) {
                ivMiniPlayStatus.setImageResource(R.drawable.svg_play);
            }
        } else if(musicStatus == PLAY_STATUS_PAUSE) {
            pauseMusic(false);
            if(ivMiniPlayStatus != null) {
                ivMiniPlayStatus.setImageResource(R.mipmap.icon_menu_pause);
            }
        } else if(musicStatus == PLAY_STATUS_ERROR || musicStatus == PLAY_STATUS_COMPLETE) {
            playUrl = "";
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void eventMsg(final EventBusBean messBean) {
        if(messBean.getType() == EventType.MUSIC_TIME_INFO){
            //时间信息
            if(musicStatus==PLAY_STATUS_PAUSE){
                pauseMusic(true);
                return;
            }
            MyLog.d("播放中...");
            if(!isPlaying) {
                if(ivMiniPlayStatus != null) {
                    ivMiniPlayStatus.setImageResource(R.mipmap.icon_menu_pause);
                }
            }
            isPlaying = true;
            timeBean = (TimeBean) messBean.getObject();
            timeInfo(timeBean);
            musicStatus = PLAY_STATUS_PLAYING;
            onMusicStatus(musicStatus);
        }else if(messBean.getType() == EventType.MUSIC_ERROR){
            String errormsg = (String) messBean.getObject();
            MyLog.d("播放失败...");
            musicStatus = PLAY_STATUS_ERROR;
            onMusicStatus(musicStatus);
        } else if(messBean.getType() == EventType.MUSIC_LOAD){
            boolean load = (boolean) messBean.getObject();
            onLoad(load);
            if(load) {
                MyLog.d("加载中...");
                musicStatus = PLAY_STATUS_LOADING;
            } else {
                MyLog.d("加载完成...");
                musicStatus = PLAY_STATUS_UNLOADING;
            }
            onMusicStatus(musicStatus);
        } else if(messBean.getType() == EventType.MUSIC_COMPLETE) {
            isPlaying = false;
            MyLog.d("播放完成...");
            musicStatus = PLAY_STATUS_COMPLETE;
            onMusicStatus(musicStatus);
        } else if(messBean.getType() == EventType.MUSIC_PAUSE_RESUME_RESULT) {
            boolean pause = (boolean) messBean.getObject();
            if(pause) {
                MyLog.d("暂停（pause）...");
                musicStatus = PLAY_STATUS_PAUSE;
            } else {
                MyLog.d("播放（resume）...");
                musicStatus = PLAY_STATUS_RESUME;
            }
            onMusicStatus(musicStatus);
        }
    }

    /**
     * 暂停播放
     * @param pause
     */
    public void pauseMusic(boolean pause) {
        if(eventPauseResumeBean == null) {
            eventPauseResumeBean = new EventBusBean(EventType.MUSIC_PAUSE_RESUME, pause);
        } else {
            eventPauseResumeBean.setType(EventType.MUSIC_PAUSE_RESUME);
            eventPauseResumeBean.setObject(pause);
        }
        isPlaying = !pause;
        EventBus.getDefault().post(eventPauseResumeBean);
    }

    public void timeInfo(TimeBean timeBean){

    }

    public void onLoad(boolean load){

    }

    public void onPlayHistoryChange(){

    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setCdRadio(float radio) {
        cdRadio = radio;
    }

    public float getCdRadio() {
        return cdRadio;
    }


    public PlayBean getPlayBean() {
        if(playBean == null) {
            playBean = new PlayBean();
        }
        MyLog.d("url is :" + playBean.getUrl());
        return playBean;
    }

    public static TimeBean getTimeBean() {
        if(timeBean == null) {
            timeBean = new TimeBean();
        }
        return timeBean;
    }

    public int getProgress() {
        if(timeBean != null && timeBean.getTotalSecs() > 0) {
            return timeBean.getCurrSecs() * 100 / timeBean.getTotalSecs();
        }
        return 0;
    }

    public void onMusicStatus(int status) {
        switch (status) {
            case PLAY_STATUS_ERROR:
                if(ivMiniPlayStatus != null) {
                    ivMiniPlayStatus.setImageResource(R.drawable.svg_play);
                }
                break;
            case PLAY_STATUS_LOADING:
                if(ivMiniPlayStatus != null) {
                    ivMiniPlayStatus.setVisibility(View.GONE);
                }
                break;
            case PLAY_STATUS_UNLOADING:
                if(ivMiniPlayStatus != null) {
                    ivMiniPlayStatus.setVisibility(View.VISIBLE);
                }
                break;
            case PLAY_STATUS_PLAYING:
                if(ivMiniPlayStatus != null) {
                    ivMiniPlayStatus.setImageResource(R.mipmap.icon_menu_pause);
                    ivMiniPlayStatus.setVisibility(View.VISIBLE);
                }
                break;
            case PLAY_STATUS_PAUSE:
                if(ivMiniPlayStatus != null) {
                    ivMiniPlayStatus.setImageResource(R.drawable.svg_play);
                    ivMiniPlayStatus.setVisibility(View.VISIBLE);
                }
                break;
            case PLAY_STATUS_RESUME:
                if(ivMiniPlayStatus != null) {
                    ivMiniPlayStatus.setImageResource(R.drawable.svg_play);
                }
                break;
            case PLAY_STATUS_COMPLETE:
                if(ivMiniPlayStatus != null) {
                    ivMiniPlayStatus.setImageResource(R.drawable.svg_play);
                }
                break;
            default:
                break;
        }
    }

    public void onRelease() {
        eventPauseResumeBean = null;
        cdRadio = 0f;
        playBean = null;
        timeBean = null;
        isPlaying = false;
        playUrl = "";
        musicStatus = -1;
    }

    public void playNext(boolean next) {
        if(FileUtil.fileList != null && FileUtil.fileList.size() > 0) {
            int size = FileUtil.fileList.size();
            for(int i = 0; i < size; i++) {
                Mp3FileBean mp3File = FileUtil.fileList.get(i);
                //当前播放的节目
                if(mp3File.index == getPlayBean().getIndex()){
                    if(next) {
                        if(i == size - 1) {
                            showToast("已经全部播放完了");
                        } else if(i < size - 1) {
                            mp3File = FileUtil.fileList.get(i+1);
                            getPlayBean().setName(mp3File.name);
                            getPlayBean().setUrl(mp3File.path);
                            getPlayBean().setIndex(mp3File.index);
                            getPlayBean().setDuration((int)mp3File.duration);
                            playMusic();
                            onPlayHistoryChange();
                        }
                        break;
                    } else {
                        if(i == 0) {
                            showToast("已经到头了");
                        } else if(i > 0) {
                            mp3File = FileUtil.fileList.get(i-1);
                            getPlayBean().setName(mp3File.name);
                            getPlayBean().setUrl(mp3File.path);
                            getPlayBean().setIndex(mp3File.index);
                            getPlayBean().setDuration((int)mp3File.duration);
                            playMusic();
                            onPlayHistoryChange();
                        }
                        break;
                    }
                }
            }
        } else {
            showToast("没有文件可以播放");
        }
    }

    public void playNextMusic(){
        playNext(true);
    }

    public void playMusic(){

    }

}
