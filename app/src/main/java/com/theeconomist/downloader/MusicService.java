package com.theeconomist.downloader;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.theeconomist.downloader.bean.EventBusBean;
import com.theeconomist.downloader.bean.SeekBean;
import com.theeconomist.downloader.bean.TimeBean;
import com.theeconomist.downloader.log.MyLog;
import com.theeconomist.downloader.utils.EventType;
import com.ywl5320.wlmedia.WlMedia;
import com.ywl5320.wlmedia.enums.WlPlayModel;
import com.ywl5320.wlmedia.listener.WlOnCompleteListener;
import com.ywl5320.wlmedia.listener.WlOnErrorListener;
import com.ywl5320.wlmedia.listener.WlOnLoadListener;
import com.ywl5320.wlmedia.listener.WlOnPauseListener;
import com.ywl5320.wlmedia.listener.WlOnPreparedListener;
import com.ywl5320.wlmedia.listener.WlOnTimeInfoListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MusicService extends Service {
    private WlMedia wlMedia;
    private String url;
    private double duration;
    private EventBusBean timeEventBean;
    private EventBusBean errorEventBean;
    private EventBusBean loadEventBean;
    private EventBusBean completeEventBean;
    private EventBusBean pauseResumeEventBean;

    public MusicService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        wlMedia = new WlMedia();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(wlMedia != null) {
            wlMedia.stop();
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
        wlMedia.setSource(url);
        wlMedia.setVolume(100);
        wlMedia.setPlayModel(WlPlayModel.PLAYMODEL_ONLY_AUDIO);
        wlMedia.setOnPreparedListener(new WlOnPreparedListener() {
            @Override
            public void onPrepared() {
                MyLog.e("onPrepared.................");
                wlMedia.start();
                duration=wlMedia.getDuration();
            }
        });

        wlMedia.setOnTimeInfoListener(new WlOnTimeInfoListener() {
            @Override
            public void onTimeInfo(double time) {
                TimeBean timeBean=new TimeBean();
                timeBean.setCurrSecs((int)Math.floor(time));
                timeBean.setTotalSecs((int)duration);
                if(timeEventBean == null) {
                    timeEventBean = new EventBusBean(EventType.MUSIC_TIME_INFO, timeBean);
                } else {
                    timeEventBean.setObject(timeBean);
                    timeEventBean.setType(EventType.MUSIC_TIME_INFO);
                }
                EventBus.getDefault().post(timeEventBean);
            }
        });

        wlMedia.setOnErrorListener(new WlOnErrorListener() {
            @Override
            public void onError(int code, String msg) {
                if(errorEventBean == null) {
                    errorEventBean = new EventBusBean(EventType.MUSIC_ERROR, msg);
                } else {
                    errorEventBean.setType(EventType.MUSIC_ERROR);
                    errorEventBean.setObject(msg);
                }
                EventBus.getDefault().post(errorEventBean);
                url = "";
            }
        });

        wlMedia.setOnLoadListener(new WlOnLoadListener() {
            @Override
            public void onLoad(boolean load) {
                if(loadEventBean == null) {
                    loadEventBean = new EventBusBean(EventType.MUSIC_LOAD, load);
                } else {
                    loadEventBean.setType(EventType.MUSIC_LOAD);
                    loadEventBean.setObject(load);
                }
                EventBus.getDefault().post(loadEventBean);
            }
        });

        wlMedia.setOnCompleteListener(new WlOnCompleteListener() {
            @Override
            public void onComplete() {
                if(completeEventBean == null) {
                    completeEventBean = new EventBusBean(EventType.MUSIC_COMPLETE, true);
                } else {
                    completeEventBean.setType(EventType.MUSIC_COMPLETE);
                    completeEventBean.setObject(true);
                }
                EventBus.getDefault().post(completeEventBean);
                url = "";
            }
        });

        wlMedia.setOnPauseListener(new WlOnPauseListener() {
            @Override
            public void onPause(boolean pause) {
                if(pauseResumeEventBean == null) {
                    pauseResumeEventBean = new EventBusBean(EventType.MUSIC_PAUSE_RESUME_RESULT, pause);
                } else {
                    pauseResumeEventBean.setType(EventType.MUSIC_PAUSE_RESUME_RESULT);
                    pauseResumeEventBean.setObject(pause);
                }
                EventBus.getDefault().post(pauseResumeEventBean);
            }
        });

        wlMedia.prepared();

        return super.onStartCommand(intent, flags, startId);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void eventMsg(final EventBusBean messBean) {
        if(messBean.getType() == EventType.MUSIC_PAUSE_RESUME) {
            boolean pause = (boolean) messBean.getObject();
            if(pause) {
                wlMedia.pause();
            } else {
                wlMedia.resume();
            }
        } else if(messBean.getType() == EventType.MUSIC_NEXT) {
            if(wlMedia != null) {
                String u = (String) messBean.getObject();
                if(!url.equals(u)) {
                    url = u;
                    wlMedia.setSource(url);
                    wlMedia.next();
                }
            }
        } else if(messBean.getType() == EventType.MUSIC_SEEK_TIME) {
            if(wlMedia != null) {
                SeekBean seekBean = (SeekBean) messBean.getObject();
                wlMedia.seek(seekBean.getPosition());
            }
        } else if(messBean.getType()== EventType.MUSIC_STOP){
            if(wlMedia != null) {
                wlMedia.stop();
                wlMedia=null;
            }
        }
    }
}
