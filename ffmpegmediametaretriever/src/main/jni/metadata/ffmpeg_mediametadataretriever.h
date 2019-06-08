//
// Created by Administrator on 2019/6/1.
//

#ifndef NDK_FFMPEG_MEDIAMETADATARETRIEVER_H
#define NDK_FFMPEG_MEDIAMETADATARETRIEVER_H

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/dict.h>

#include <android/native_window_jni.h>

typedef enum
{
    OPTION_PREVIOUS_SYNC=0,
    OPTION_NEXT_SYNC=1,
    OPTION_CLOSET_SYNC=2,
    OPTION_CLOSET=3,
} Options;

typedef struct State
{
    AVFormatContext *pFormatCtx;
    int audio_stream;
    int video_stream;
    AVStream *audio_st;
    AVStream *video_st;
    int fd;
    int64_t offset;
    const char *headers;
    struct SwsContexy *sws_ctx;
    AVCodecContext *codecCtx;

    struct SwsContext *scaled_sws_ctx;
    AVCodecContext *scaled_codecCtx;
    ANativeWindow *native_window;
} State;

struct AVDictionary
{
    int count;
    AVDictionaryEntry *elems;
};

int set_data_source_uri(State **ps, const char *path, const char* headers);
int set_data_source_fd(State **ps, int fd, int64_t offset, int64_t length);
const char* extract_metadata(State **ps, const char* key);
const char* extract_metadata_from_chapter(State **ps, const char* key, int chapter);
int get_metadata(State **ps, AVDictionary **metadata);
int get_embedded_picture(State **ps, AVPacket *pkt);
int get_frame_at_time(State **ps, int64_t timeUs, int option, AVPacket *pkt);
int get_scaled_frame_at_time(State **ps, int64_t timeUs, int option, AVPacket *pkt, int width, int height);
int set_native_window(State **ps, ANativeWindow* native_window);
void release(State **ps);

#endif //NDK_FFMPEG_MEDIAMETADATARETRIEVER_H
