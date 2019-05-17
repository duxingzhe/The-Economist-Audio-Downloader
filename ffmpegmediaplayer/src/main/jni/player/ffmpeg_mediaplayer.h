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

typedef enum media_event_type
{
    MEDIA_NOP=0,
    MEDIA_PREPARED=1,
    MEDIA_PLAYBACK_COMPLETE=2,
    MEDIA_BUFFERING_UPDATE=3,
    MEDIA_SEEK_COMPLETE=4,
    MEDIA_ERROR=100,
    MEDIA_INFO=200,
} media_event_type;

typedef enum media_info_type
{
    MEDIA_INFO_UNKNOWN=0,
    MEDIA_INFO_VIDEO_TRACK_LAGGING=700,
    MEDIA_INFO_VIDEO_RENDERING_START=3,
    MEDIA_INFO_BUFFERING_START=701,
    MEDIA_INFO_BUFFERING_END=702,
    MEDIA_INFO_NETWORK_BANDWIDTH=703,
    MEDIA_INFO_BAD_INTERLEAVING=800,
    MEDIA_INFO_NOT_SEEKABLE=801,
    MEDIA_INFO_METADATA_UPDATE=802,
    MEDIA_INFO_UNSUPPORTED_SUBTITLE=901,
    MEDIA_INFO_SUBTITLE_TIMED_OUT=902,
} media_info_type;

typedef struct PacketQueue
{
    SDL_Window *screen;
    SDL_Renderer *renderer;
    SDL_Texture *texture;
    int initialized;
    AVPacketList *first_pkt, *last_pkt;
    int nb_packets;
    int size;
    SDL_mutex* mutex;
    SDL_cond *cond;
} PacketQueue;

typedef struct VideoState
{
    AVFormatContext *pFormatCtx;
    int videoStream, audioStream;

    int av_sync_type;
    double external_clock;
    int64_t external_clock_time;
    int seek_req;
    int seek_flags;
    int64_t seek_pos;
    int64_t seek_rel;

    double audio_clock;
    AVStream *audio_st;
    PacketQueue audioq;
    AVFrame audio_frame;
    uint8_t audio_buf[(MAX_AUDIO_FRAME_SIZE * 3)/2];
    unsigned int audio_buf_size;
    unsigned int audio_buf_index;
    AVPacket audio_pkt;
    uint8_t *audio_pkt_data;

    struct AudioPlayer *audio_player;
    void (*audio_callback)(void *userdata, uint8_t *stream, int len);
} VideoState;

#endif //NDK_FFMPEG_MEDIAPLAYER_H
