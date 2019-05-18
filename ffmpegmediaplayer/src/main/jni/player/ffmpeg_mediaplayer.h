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

typedef int media_error_type;
static const media_error_type MEDIA_ERROR_UNKNOWN=1;
static const  media_error_type MEIDA_ERROR_SERVER_DIED=100;

typedef enum
{
    MEDIA_PLAYER_STATE_ERROR=0,
    MEDIA_PLAYER_IDLE=1<<0,
    MEDIA_PLAYER_INITIALIZED=1<<1,
    MEDIA_PLAYER_PREPARING=1<<2,
    MEDIA_PLAYER_PREPARED=1<<3,
    MEDIA_PLAYER_STARTED=1<<4;
    MEDIA_PLAYER_PAUSED=1<<5;
    MEDIA_PLAYER_STOPPED=1<<6;
    MEDIA_PLAYER_PLAYBACK_COMPLETE=1<<7;
} media_player_states;

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

typedef struct Picture
{
    int linesize;
    void *buffer;
} Picture;
typedef struct VideoPicture
{
    Picture *bmp;
    int width, height;
    int allocated;
    double pts;
} VideoPicture;

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
    int audio_pkt_size;
    int audio_hw_buf_size;
    double audio_diff_cum;
    double audio_diff_avg_coef;
    double audio_diff_threashold;
    int audio_diff_avg_count;
    double frame_timer;
    double frame_last_pts;
    double frame_last_delay;
    double video_clock;
    double video_current_pts;
    int64_t video_current_pts_time;
    AVStream *video_st;
    PacketQueue videoq;
    VideoPicture pictq[VIDEO_PICTURE_QUEUE_SIZE];
    int pictq_size, pictq_rindex, pictq_windex;
    SDL_mutex *pictq_mutex;
    SDL_cond *pictq_cond;
    pthread_t *parse_tid;
    pthread_t *video_tid;
    pthread_t *video_refresh_tid;

    char filename[1024];
    int quit;

    AVIOContext *io_context;
    struct SwsContext *sws_ctx;
    struct swrContext *sws_ctx_audio;
    struct AudioPlayer *audio_player;
    struct VideoPlayer *video_player;
    void (*audio_callback)(void *userdata, uint8_t *stream, int len);
    int prepared;

    char headers[2048];

    int fd;
    int64_t offset;
    int prepare_sync;

    void (*notify_callback)(void*, int, int, int, int);
    void* clazz;

    int read_pause_return;
    int last_pasued;

    pthread_t *tid;
    int player_started;
    AVPacket flush_pkt;
    void *net;
    void *native_window;

    int stream_type;
} VideoState;

struct AVDictionary
{
    int count;
    AVDictionaryEntry *elems;
};

typedef struct Message
{
    VideoState* is;
    int msg;
    int ext1;
    int ext2;
    int from_thread;
} Message;

enum
{
    AV_SYNC_AUDIO_MASTER,
    AV_SYNC_VIDEO_MASTER,
    AV_SYNC_EXTERNAL_MASTER,
};

int private_main(int argc, char *argv[]);

VideoState *create();
VideoState *getNextMediaPlayer(VideoState **ps);
void disconnect(VideoState **ps);
int setDataSourceURI(VideoState **ps, const char *url, const char *headers);
int setDataSourceFD(VideoState **ps, int fd, int64_t offset, int64_t length);
int setVideoSurface(VideoState **ps, void *native_window);
int setListener(VideoState **ps, void* clazz, void (*listener)(void*, int, int, int, int));
int setMetadataFilter(VideoState **ps, char *allow[], char *block[]);
int getMetadata(VideoState **ps, AVDictionary **metadata);
int prepare(VideoState **ps);
int prepareAsync(VideoState **ps);
int start(VideoState **ps);
int stop(VideoState **ps);
int pause_l(VideoState **ps);
int isPlaying(VideoState **ps);
int getVideoWidth(VideoState **ps, int *w);
int getVideoHeight(VideoState **ps, int *h);
int seekTo(VideoState **ps, int msec);
int getCurrentPosition(VideoState **ps, int *mesc);
int getDuration(VideoState **ps, int msec);
int reset(VideoState **ps);
int setAudioStreamType(VideoState **ps, int type);
int setLooping(VideoState **ps, int type);
int isLooping(VideoState **ps);
int setVolume(VideoState **ps, float leftVolume, float rightVolume);
void notify(VideoState *is, int msg, int ext1, int ext2);
void notify_from_thread(VideoState *is, int msg, int ext1, int ext2);
int setNextPlayer(VideoState **ps, VideoState *next);

void clear_l(VideoState **ps);
int setTo_l(VideoState **ps, int msec);
int prepareAsync_l(VideoState **ps);
int getDuration_l(VideoState **ps, int *msec);

#endif //NDK_FFMPEG_MEDIAPLAYER_H
