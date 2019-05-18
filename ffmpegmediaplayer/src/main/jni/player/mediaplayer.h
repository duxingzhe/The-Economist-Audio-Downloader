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
};
#endif //NDK_MEDIAPLAYER_H
