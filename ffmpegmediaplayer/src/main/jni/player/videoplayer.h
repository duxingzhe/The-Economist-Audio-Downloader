//
// Created by Luxuan on 2019/5/14.
//

#ifndef NDK_VIDEOPLAYER_H
#define NDK_VIDEOPLAYER_H

#include <stdint.h>
#include <stdio.h>

#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <ffmpeg_mediaplayer.h>
#include <android/native_window_jni.h>

typedef struct VideoPlayer
{
    ANativeWindow* nativeWindow;
} VideoPlayer;

void createVideoEngine(VideoPlayer **ps);
void createScreen(VideoPlayer **ps, void *surface, int width, int height);
void setSurface(VideoPlayer **ps, void *surface);
struct SwsContext *createScaler(VideoPlayer **ps, AVCodecContext *codec);
void *createBmp(VideoPlayer **ps, int width, int height);
void *destroyBmp(VideoPlayer **ps, void *bmp);
void updateBmp(VideoPlayer **ps, struct SwsContext *sws_ctx, AVCodecContext *pCodecCtx, void *bmp, AVFrame *pFrame, int width, int height);
void displayBmp(VideoPlayer **ps, void *bmp, AVCodecContext *pCodecCtx, int width, int height);
void shutdownVideoEngine(VideoPlayer **ps);

#endif //NDK_VIDEOPLAYER_H
