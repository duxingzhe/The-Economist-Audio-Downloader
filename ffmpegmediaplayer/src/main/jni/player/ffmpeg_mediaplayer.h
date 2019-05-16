//
// Created by Luxuan on 2019/5/14.
//

#ifndef NDK_FFMPEG_MEDIAPLAYER_H
#define NDK_FFMPEG_MEDIAPLAYER_H

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavformat/avio.h>
#include <libswresample/swresample.h>
#include <libavutil/avstring.h>
#include <libavutil/opt.h>
#include <libavutil/time.h>
#include <libavutil/dict.h>

#include <SDL.h>
#include <SDL_thread.h>

#include <stdio.h>
#include <math.h>

#include <pthread.h>
#include "audioplayer.h"
#include "videoplayer.h"
#include <unistd.h>
#include "Errors.h"

#include "ffmpeg_utils.h"

#define SDL_AUDIO_BUFF3ER_SIZE 1024
#define MAX_AUDIO_FRAME_SIZE 192000
#define MAX_AUDIO_SZIE (5*16*1024)
#define MAX_VIDEO_SIZE (5*256*1024)
#define AV_SYNC_THRESHOLD 0.01
#define AV_NOSYNC_THRESHOLD 10.0
#define SAMPLE_CORRECTION_PERCENT_MAX 10
#define AUDIO_DIFF_AVG_NB 20
#define FF_ALLOC_EVENT (24)
#define FF_REFRESH_EVENT (24+1)
#define FF_QUIT_EVENT (24+2)
#define VIDEO_PICTURE_QUEUE_SIZE 1
#define DEFAULT_AV_SYNC_TYPE AV_SYNC_VIDEO_MASTER

typedef struct VideoState
{
    AVFormatContext *pFormatCtx;
    struct AudioPlayer *audio_player;
    void (*audio_callback)(void *userdata, uint8_t *stream, int len);
} VideoState;

#endif //NDK_FFMPEG_MEDIAPLAYER_H
