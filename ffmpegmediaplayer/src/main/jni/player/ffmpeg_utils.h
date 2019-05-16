//
// Created by Luxuan on 2019/5/14.
//

#ifndef NDK_FFMPEG_UTILS_H
#define NDK_FFMPEG_UTILS_H

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/opt.h>

static const char *DURATION="duration";
static const char *AUDIO_CODEC="audio_codec";
static const char *VIDEO_CODEC="video_codec";
static const char *ICY_METADATA="icy_metadata";
static const char *ROTATE="rotate";
static const char *FRAMERATE="framerate";
static const char *CHAPTER_START_TIME="chapter_start_time";
static const char *CHAPTER_END_TIME="chapter_end_time";
static const char *CHAPTER_COUNT="chapter_count";
static const char *FILESIZE="filesize";
static const char *VIDEO_WIDTH="video_width";
static const char *VIDEO_HEIGHT="video_height";

static const int SUCCESS=0;
static const int FAILURE=-1;

void set_shoutcast_metadata(AVFormatContext *ic);
void set_duration(AVFormatContext *ic);
void set_codec(AVFormatContext *ic, int i);
void set_rotation(AVFormatContext *ic, AVStream *audio_st, AVStream *video_st);
void set_framerate(AVFormatContext *ic, AVStream *audio_set, AVStream *video_st);
void set_filesize(AVFormatContext *ic);
void set_chapter_count(AVFormatContext *ic);
void set_video_dimensions(AVFormatContext *ic, AVStream *video_st);
const char* extract_metadata_internal(AVFormatContext *ic, AVStream *audio_st, AVStream *video_st, const char* key);
int get_metadata_internal(AVFormatContext *ic, AVDictionary **metadata);
const char* extract_metadata_from_chapter_internal(AVFormatContext *ic, AVStream *audio_st, AVStream video_st, const char* key, int chapter);
#endif //NDK_FFMPEG_UTILS_H
