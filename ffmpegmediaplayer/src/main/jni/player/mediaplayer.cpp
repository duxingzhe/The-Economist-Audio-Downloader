//
// Created by Administrator on 2019/5/16.
//

#define LOG_TAG "FFmpegMediaPlayer"

#include <sys/types.h>
#include <sys/stat.h>
#include "Errors.h"
#include <pthread.h>
#include "mediaplayer.h"

extern "C"
{
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "ffmpeg_mediaplayer.h"
}

using namespace std;

MediaPlayer::MediaPlayer()
{

}

