package com.theeconomist.downloader.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;

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

/**
 * Created by ywl on 2018/1/12.
 */

public abstract class BaseMusicActivity extends BaseActivity{

    //暂停、播放状态
    private static EventBusBean eventPauseResumeBean;
    private static float cdRadio = 0f;
    private static PlayBean playBean;
    private static TimeBean timeBean;
    private static boolean isPlaying = false;
    private boolean isExiting=false;

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
        onMusicStatus(musicStatus);
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

    public boolean isExiting(){
        return isExiting;
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

    public void setIsExiting(boolean isExiting){
        this.isExiting=isExiting;
    }

}
