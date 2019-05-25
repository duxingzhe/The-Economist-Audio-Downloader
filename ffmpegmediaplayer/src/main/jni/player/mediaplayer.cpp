//
// Created by Administrator on 2019/5/16.
//

#define LOG_TAG "FFmpegMediaPlayer"

#include <sys/types.h>
#include <sys/stat.h>
#include "Errors.h"
#include <pthread.h>
#include "mediaplayer.h"

extern "C"
{
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "ffmpeg_mediaplayer.h"
}

using namespace std;

MediaPlayer::MediaPlayer()
{
    state=NULL;

    mListener=NULL;
    mCookie=NULL;
    mDuration=-1;
    mStreamType=3;
    mSeekPosition=-1;
    mCurrentState=MEDIA_PLAYER_IDLE;
    mPrepareSync=false;
    mPrepareStatus=NO_ERROR;
    mLoop=false;
    mLeftVolume=mRightVolume=1.0;
    mVideoWidth=mVideoHeight=0;

    mAudioSessionId=0;
    mSendLevel=0;
}

MediaPlayer::~MediaPlayer()
{
    disconnect();
}

void MediaPlayer::disconnect()
{
    VideoState *p=NULL;

    {
        Mutex::Autolock _l(mLock);
        p=state;
        ::reset(&p);
    }

    if(state!=0)
    {
        ::disconnect(&state);
    }
}

void MediaPlayer::clear_l()
{
    mDuration=-1;
    mCurrentPosition=-1;
    mSeekPosition=-1;
    mVideoWidth=mVideoHeight=0;
}

static void notifyListener(void *clazz, int msg, int ext1, int ext2, int fromThread)
{
    MediaPlayer *mp=(MediaPlayer *)clazz;
    mp->notify(msg, ext1, ext2, fromThread);
}

status_t MediaPlayer::setListener(MediaPlayerListener *listener)
{
    Mutex::Autolock _l(mLock);
    mListener=listener;

    if(state!=0)
    {
        ::setListener(&state, this, notifyListener);
    }

    return NO_ERROR;
}

MediaPlayerListener *MediaPlayer::getListener()
{
    return mListener;
}

