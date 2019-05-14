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
#endif //NDK_FFMPEG_MEDIAPLAYER_H
