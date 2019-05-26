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

status_t MediaPlayer::setDataSource(VideoState *player)
{
    status_t err=UNKNOWN_ERROR;
    VideoState *p;

    {
        Mutex::Autolock _l(mLock);

        if(!((mCurrentState&MEDIA_PLAYER_IDLE)||(mCurrentState==MEDIA_PLAYER_STATE_ERROR)))
        {
            return INVALID_OPERATION;
        }

        ::clear_l(&player);
        ::setListener(&player, this, notifyListener);
        clear_l();
        p=state;
        state=player;
        if(player!=0) {
            mCurrentState = MEDIA_PLAYER_INITIALIZED;
            err = NO_ERROR;
        }
        else
        {

        }
    }

    if(p!=0)
    {
        ::disconnect(&p);
    }

    return err;
}

status_t MediaPlayer::setDataSource(const char *url, const char *headers)
{
    status_t err=BAD_VALUE;
    if(url!=NULL)
    {
        VideoState* state=::create();
        err=::setDataSourceURI(&state, url, headers);
        if(err==NO_ERROR)
        {
            err=setDataSource(state);
        }
    }

    return err;
}

status_t MediaPlayer::setMetadataFilter(char *allow[], char *block[])
{
    Mutex::Autolock lock(mLock);
    if(state==NULL)
    {
        return NO_INIT;
    }

    return ::setMetadataFilter(&state, allow, block);
}

status_t MediaPlayer::getMetadata(bool update_only, bool apply_filter, AVDictionary **metadata)
{
    Mutex::Autolock lock(mLock);
    if(state==NULL)
    {
        return NO_INIT;
    }

    return ::getMetadata(&state, metadata);
}

status_t MediaPlayer::setVideoSurface(void *native_window)
{
    Mutex::Autolock _l(mLock);
    if(state==0)
        return NO_INIT;
    if(native_window!=NULL)
        return ::setVideoSurface(&state, native_window);
    else
        return ::setVideoSurface(&state, NULL);
}

status_t MediaPlayer::prepareAsync_l()
{
    if((state!=0)&&(mCurrentState&(MEDIA_PLAYER_INITIALIZED | MEDIA_PLAYER_STOPPED)))
    {
        mCurrentState=MEDIA_PLAYER_PREPARING;
        return ::prepareAsync(&state);
    }

    return INVALID_OPERATION;
}

status_t MediaPlayer::prepare()
{
    Mutex::Autolock _l(mLock);

    if(mPrepareSync)
    {
        return -EALREADY;
    }

    mPrepareSync=true;
    status_t ret=::prepare(&state);

    if(ret!=NO_ERROR)
    {
        return ret;
    }

    if(mPrepareSync)
    {
        mPrepareSync=false;
    }

    return mPrepareStatus;
}

status_t MediaPlayer::start()
{
    Mutex::Autolock _l(mLock);
    if(mCurrentState & MEDIA_PLAYER_STARTED)
        return NO_ERROR;
    if((state!=0)&&(mCurrentState & (MEDIA_PLAYER_PREPARED | MEDIA_PLAYER_PLAYBACK_COMPLETE
            | MEDIA_PLAYER_PAUSED)))
    {
        ::setLooping(&state, mLoop);
        ::setVolume(&state, mLeftVolume, mRightVolume);

        mCurrentState=MEDIA_PLAYER_STARTED;
        status_t ret=::start(&state);

        if(ret!=NO_ERROR)
        {
            mCurrentState=MEDIA_PLAYER_STATE_ERROR;
        }
        else
        {
            if(mCurrentState==MEDIA_PLAYER_PLAYBACK_COMPLETE)
            {

            }
        }
        return ret;
    }

    return INVALID_OPERATION;
}

status_t MediaPlayer::stop()
{
    Mutex::Autolock _l(mLock);
    if(mCurrentState & MEDIA_PLAYER_STOPPED)
        return NO_ERROR;
    if((state!=0)&&(mCurrentState &(MEDIA_PLAYER_STARTED| MEDIA_PLAYER_PREPARED |
                        MEDIA_PLAYER_PAUSED | MEDIA_PLAYER_PLAYBACK_COMPLETE)))
    {
        status_t ret=::stop(&state);
        if(ret!=NO_ERROR)
        {
            mCurrentState=MEDIA_PLAYER_STATE_ERROR;
        }
        else
        {
            mCurrentState=MEDIA_PLAYER_STOPPED;
        }

        return ret;
    }

    return INVALID_OPERATION;
}

status_t MediaPlayer::pause()
{
    Mutex::Autolock _l(mLock);
    if(mCurrentState&(MEDIA_PLAYER_PAUSED| MEDIA_PLAYER_PLAYBACK_COMPLETE))
    {
        return NO_ERROR;
    }
    if((state!=0)&&(mCurrentState & MEDIA_PLAYER_STARTED))
    {
        status_t ret=::pause_l(&state);
        if(ret!=NO_ERROR)
        {
            mCurrentState=MEDIA_PLAYER_STATE_ERROR;
        }
        else
        {
            mCurrentState=MEDIA_PLAYER_PAUSED;
        }

        return ret;
    }

    return INVALID_OPERATION;
}

bool MediaPlayer::isPlaying()
{
    Mutex::Autolock _l(mLock);
    if(state!=0)
    {
        bool temp=false;

        if(::isPlaying(&state))
        {
            temp=true;
        }

        if((mCurrentState&MEDIA_PLAYER_STARTED) && !temp)
        {
            mCurrentState=MEDIA_PLAYER_PAUSED;
        }

        return temp;
    }

    return false;
}

status_t MediaPlayer::getVideoWidth(int *w)
{
    Mutex::Autolock _l(mLock);
    if(state==0)
        return INVALID_OPERATION;
    *w=mVideoWidth;
    ::getVideowidth(&state, w);
    return NO_ERROR;
}

status_t MediaPlayer::getVideoHeight(int *h)
{
    Mutex::Autolock _l(mLock);
    if(state==0)
    {
        return INVALID_OPERATION;
    }

    *h=mVideoHieght;
    ::getVideoHeight(&state, h);
    return NO_ERROR;
}
