package com.theeconomist.downloader;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.theeconomist.downloader.bean.EventBusBean;
import com.theeconomist.downloader.bean.SeekBean;
import com.theeconomist.downloader.bean.TimeBean;
import com.theeconomist.downloader.log.MyLog;
import com.theeconomist.downloader.utils.EventType;
import com.ywl5320.wlmedia.WlPlayer;
import com.ywl5320.wlmedia.enums.WlCompleteType;
import com.ywl5320.wlmedia.enums.WlLoadStatus;
import com.ywl5320.wlmedia.enums.WlPlayModel;
import com.ywl5320.wlmedia.listener.WlOnMediaInfoListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MusicService extends Service {

    private WlPlayer wlPlayer;
    private String url;
    private double duration;
    private EventBusBean timeEventBean;
    private EventBusBean loadEventBean;
    private EventBusBean completeEventBean;

    public MusicService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        wlPlayer = new WlPlayer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(wlPlayer != null) {
            wlPlayer.stop();
        }
        EventBus.getDefault().unregister(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        url = intent.getStringExtra("url");
        wlPlayer.setSource(url);
        wlPlayer.setVolume(100);
        wlPlayer.setPlayModel(WlPlayModel.WL_PLAY_MODEL_ONLY_AUDIO);
        wlPlayer.setOnMediaInfoListener(new WlOnMediaInfoListener() {
            @Override
            public void onPrepared() {
                MyLog.e("onPrepared.................");
                wlPlayer.start();
                duration=wlPlayer.getDuration();
            }

            @Override
            public void onTimeInfo(double v, double v1) {
                TimeBean timeBean=new TimeBean();

                if(Math.floor(v)>duration){
                    timeBean.setCurrSecs((int)duration);
                }else{
                    timeBean.setCurrSecs((int)Math.floor(v));
                }

                timeBean.setTotalSecs((int)duration);
                if(timeEventBean == null) {
                    timeEventBean = new EventBusBean(EventType.MUSIC_TIME_INFO, timeBean);
                } else {
                    timeEventBean.setObject(timeBean);
                    timeEventBean.setType(EventType.MUSIC_TIME_INFO);
                }
                EventBus.getDefault().post(timeEventBean);
            }

            @Override
            public void onComplete(WlCompleteType wlCompleteType, String s) {
                if(completeEventBean == null) {
                    completeEventBean = new EventBusBean(EventType.MUSIC_COMPLETE, true);
                } else {
                    completeEventBean.setType(EventType.MUSIC_COMPLETE);
                    completeEventBean.setObject(true);
                }
                EventBus.getDefault().post(completeEventBean);
                url = "";
            }

            @Override
            public void onLoad(WlLoadStatus wlLoadStatus, int i, long l) {
                if(loadEventBean == null) {
                    loadEventBean = new EventBusBean(EventType.MUSIC_LOAD, i);
                } else {
                    loadEventBean.setType(EventType.MUSIC_LOAD);
                    loadEventBean.setObject(wlLoadStatus);
                }
                EventBus.getDefault().post(loadEventBean);
            }

            @Override
            public void onSeekFinish() {

            }

            @Override
            public void onFirstFrameRendered() {

            }
        });
        wlPlayer.start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void eventMsg(final EventBusBean messBean) {
        if(messBean.getType() == EventType.MUSIC_PAUSE_RESUME) {
            boolean pause = (boolean) messBean.getObject();
            if(pause) {
                wlPlayer.pause();
            } else {
                wlPlayer.resume();
            }
        } else if(messBean.getType() == EventType.MUSIC_NEXT) {
            if(wlPlayer != null) {
                String u = (String) messBean.getObject();
                if(!url.equals(u)) {
                    url = u;
                    wlPlayer.setSource(url);
                    wlPlayer.start();
                }
            }
        } else if(messBean.getType() == EventType.MUSIC_SEEK_TIME) {
            if(wlPlayer != null) {
                SeekBean seekBean = (SeekBean) messBean.getObject();
                wlPlayer.seek(seekBean.getPosition());
            }
        } else if(messBean.getType()== EventType.MUSIC_STOP){
            if(wlPlayer != null) {
                wlPlayer.stop();
                wlPlayer=null;
            }
        }
    }
}
