#ifndef FFMPEGMUSIC_FFMPEGMUSIC_H
#define FFMPEGMUSIC_FFMPEGMUSIC_H

#include <jni.h>
#include <string>
#include <android/log.h>

extern "C"
{
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libswresample/swresample.h"
#include <android/native_window_jni.h>
#include <unistd.h>
}

#define LOG(FORMAT, ...) __andorid_log_print(ANDROID_LOG_ERROR,"LC", FORMAT, ##__VA_ARGS__);

int createFFmpeg(int *rate, int *channel);

int getPcm(void **pcm, size_t *pcm_size);

void realseFFmpeg();