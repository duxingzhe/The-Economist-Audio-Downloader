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
    ::getVideoWidth(&state, w);
    return NO_ERROR;
}

status_t MediaPlayer::getVideoHeight(int *h)
{
    Mutex::Autolock _l(mLock);
    if(state==0)
    {
        return INVALID_OPERATION;
    }

    *h=mVideoHeight;
    ::getVideoHeight(&state, h);
    return NO_ERROR;
}

status_t MediaPlayer::getCurrentPosition(int *msec)
{
    Mutex::Autolock _l(mLock);
    if(state!=0)
    {
        if(mCurrentPosition>=0)
        {
            *msec=mCurrentPosition;
            return NO_ERROR;
        }

        return ::getCurrentPosition(&state, msec);
    }
    return INVALID_OPERATION;
}

status_t MediaPlayer::getDuration_l(int *msec)
{
    bool isValidState=(mCurrentState &(MEDIA_PLAYER_PREPARED|MEDIA_PLAYER_STARTED|MEDIA_PLAYER_PAUSED|MEDIA_PLAYER_STOPPED|MEDIA_PLAYER_PLAYBACK_COMPLETE));
    if(state!=0&&isValidState)
    {
        status_t ret=NO_ERROR;
        if(mDuration<=0)
        {
            ret=::getDuration(&state, &mDuration);
        }
        if(msec)
            *msec=mDuration;
        return ret;
    }

    return INVALID_OPERATION;
}

status_t MediaPlayer::getDuration(int *msec)
{
    Mutex::Autolock _l(mLock);
    return getDuration_l(msec);
}

status_t MediaPlayer::seekTo_l(int msec)
{
    if((state!=0)&&(mCurrentState & (MEDIA_PLAYER_STARTED | MEDIA_PLAYER_PREPARED | MEDIA_PLAYER_PAUSED | MEDIA_PLAYER_PLAYBACK_COMPLETE)))
    {
        if(msec<0)
        {
            msec=0;
        }
        else if((mDuration>0)&&(msec>mDuration))
        {
            msec=mDuration;
        }

        mCurrentPosition=msec;
        if(mSeekPosition<0)
        {
            getDuration_l(NULL);
            mSeekPosition=msec;
            return ::seekTo(&state, msec);
        }
        else
        {
            return NO_ERROR;
        }
    }

    return INVALID_OPERATION;
}

status_t MediaPlayer::seekTo(int msec)
{
    Mutex::Autolock _l(mLock);
    status_t result= seekTo_l(msec);

    return result;
}

status_t MediaPlayer::reset()
{
    Mutex::Autolock _l(mLock);
    mLoop=false;
    if(mCurrentState==MEDIA_PLAYER_IDLE)
        return NO_ERROR;
    mPrepareSync=false;
    if(state!=0)
    {
        status_t ret=::reset(&state);
        if(ret!=NO_ERROR)
        {
            mCurrentState=MEDIA_PLAYER_STATE_ERROR;
        }
        else
        {
            mCurrentState=MEDIA_PLAYER_IDLE;
        }
        return ret;
    }
    clear_l();
    return NO_ERROR;
}

status_t MediaPlayer::setAudioStreamType(int type)
{
    Mutex::Autolock _l(mLock);
    if(mStreamType==type)
        return NO_ERROR;
    if(mCurrentState&(MEDIA_PLAYER_PREPARED|MEDIA_PLAYER_STARTED|MEDIA_PLAYER_PAUSED|MEDIA_PLAYER_PLAYBACK_COMPLETE))
    {
        return INVALID_OPERATION;
    }

    mStreamType=type;
    if(state!=0)
    {
        return ::setAudioStreamType(&state, type);
    }
    return OK;
}

status_t MediaPlayer::setLooping(int loop)
{
    Mutex::Autolock _l(mLock);
    mLoop=(loop!=0);
    if(state!=0)
    {
        return ::setLooping(&state, loop);
    }
    return OK;
}

status_t MediaPlayer::setVolume(float leftVolume, float rightVolume)
{
    Mutex::Autolock _l(mLock);
    mLeftVolume=leftVolume;
    mRightVolume=rightVolume;
    if(state!=0)
    {
        MediaPlayerListener *listener=mListener;
        if(listener!=0)
        {
            return ::setVolume(&state, leftVolume, rightVolume);
        }
    }

    return OK;
}

status_t MediaPlayer::setAudioSessionId(int sessionId)
{
    Mutex::Autolock _l(mLock);
    if(!(mCurrentState & MEDIA_PLAYER_IDLE))
    {
        return INVALID_OPERATION;
    }

    if(sessionId<0)
    {
        return BAD_VALUE;
    }
    mAudioSessionId=sessionId;
    return NO_ERROR;
}

int MediaPlayer::getAudioSessionId()
{
    Mutex::Autolock _l(mLock);
    return mAudioSessionId;
}

status_t MediaPlayer::setAuxEffectSendLevel(float level)
{
    Mutex::Autolock _l(mLock);
    mSendLevel=level;
    if(state!=0)
    {
        MediaPlayerListener *listener=mListener;
        if(listener!=0)
        {
            return 0;
        }
    }

    return OK;
}

status_t MediaPlayer::attachAuxEffect(int effectId)
{
    Mutex::Autolock _l(mLock);
    if(state==0||(mCurrentState & MEDIA_PLAYER_IDLE)||
            (mCurrentState==MEDIA_PLAYER_STATE_ERROR))
    {
        return INVALID_OPERATION;
    }

    MediaPlayerListener *listener=mListener;
    if(listener!=0)
    {
        return 0;
    }
    else
    {
        return -3;
    }
}

void MediaPlayer::notify(int msg, int ext1, int ext2, int fromThread)
{
    bool send=true;
    bool locked=false;

    if(!(msg==MEDIA_ERROR && mCurrentState == MEDIA_PLAYER_IDLE)&&state==0)
    {
        return;
    }

    switch(msg)
    {
        case MEDIA_NOP:
            break;
        case MEDIA_PREPARED:
            mCurrentState=MEDIA_PLAYER_PREPARED;
            if(mPrepareSync)
            {
                mPrepareSync=false;
                mPrepareStatus=NO_ERROR;
            }
            break;
        case MEDIA_PLAYER_PLAYBACK_COMPLETE:
            if(mCurrentState==MEDIA_PLAYER_IDLE)
            {

            }
            if(!mLoop)
            {
                mCurrentState=MEDIA_PLAYER_PLAYBACK_COMPLETE;
            }
            break;
        case MEDIA_ERROR:
            mCurrentState=MEDIA_PLAYER_STATE_ERROR;
            if(mPrepareSync)
            {
                mPrepareSync=false;
                mPrepareStatus=ext1;
                send=false;
            }
            break;
        case MEDIA_INFO:
            break;
        case MEDIA_SEEK_COMPLETE:
            if(mSeekPosition!=mCurrentPosition)
            {
                mSeekPosition=-1;
                seekTo_l(mCurrentPosition);
            }
            else
            {
                mCurrentPosition=mSeekPosition=-1;
            }
            break;
        case MEDIA_BUFFERING_UPDATE:
            break;
        default:
            break;
    }

    MediaPlayerListener *listener=mListener;

    if((listener!=0)&&send)
    {
        Mutex::Autolock _l(mNotifyLock);
        listener->notify(msg, ext1, ext2, fromThread);
    }
}

status_t MediaPlayer::setNextMediaPlayer(const MediaPlayer *player)
{
    if(state==NULL)
    {
        return NO_INIT;
    }
    return ::setNextPlayer(&state, next==NULL?NULL: next->state);
}