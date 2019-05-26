//
// Created by Administrator on 2019/5/18.
//

#ifndef NDK_MEDIAPLAYER_H
#define NDK_MEDIAPLAYER_H

#include "Errors.h"
#include <pthread.h>
#include "Mutex.h"

extern "C"
{
#include "ffmpeg_mediaplayer.h"
}

class MediaPlayerListener
{
public:
    virtual void notify(int msg, int ext1, int ext2, int formatThread)=0;
};

class MediaPlayer
{
public:
    MediaPlayer();
    ~MediaPlayer();
    
    void disconnect();
    status_t setDataSource(const char *url, const char *headers);
    status_t setDataSource(int fd, int64_t offset, int64_t length);
    status_t setMetadataFilter(char *allow[], char *block[]);
    status_t getMetadata(bool update_only, bool apply_filter, AVDictionary **metadata);
    status_t setVideoSurface(void* native_window);
    status_t setListener(MediaPlayerListener *listener);
    MediaPlayerListener *getListener();
    status_t prepare();
    status_t prepareAsync();
    status_t start();
    status_t stop();
    status_t pause();
    bool isPlaying();
    status_t getVideoWidth(int *w);
    status_t getVideoHeight(int *h);
    status_t seekTo(int msec);
    status_t getCurrentPosition(int *msec);
    status_t getDuration(int *msec);
    status_t reset();
    status_t setAudioStreamType(int type);
    status_t setLooping(int loop);
    bool isLooping();
    status_t setVolume(float leftVolume, float rightVolume);
    void notify(int msg, int ext1, int ext, int fromThread);
    status_t setAudioSessionId(int sessionId);
    int getAudioSessionId();
    status_t setAuxEffectSendLevel(float level);
    int attachAuxEffect(int effectId);
    status_t setnextMediaPlayer(const MediaPlayer* player);
    VideoState *state;

private:
    void clear_l();
    status_t seekTo_l(int msec);
    status_t prepareAsync_l();
    status_t getDuration_l(int *msec);
    status_t setDataSource(VideoState *state);

    Mutex mLock;
    Mutex mNotifyLock;
    MediaPlayerListener* mListener;
    void* mCookie;
    media_player_states mCurrentState;
    int mDuration;
    int mCurrentPosition;
    int mSeekPosition;
    bool mPrepareSync;
    status_t mPrepareStatus;
    int mStreamType;
    bool mLoop;
    float mLeftVolume;
    float mRightVolume;
    int mVideoWidth;
    int mVideoHeight;
    int mAudioSessionId;
    float mSendLevel;
};
#endif //NDK_MEDIAPLAYER_H
