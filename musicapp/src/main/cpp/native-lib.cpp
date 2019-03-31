#include <jni.h>
#include <string>
#include <android/log.h>

extern "C"
{
#include "libavcodec/avcodec.h"
#include "libswresample/swresample.h"
#include "libswscale/swscale.h"
#include <android/native_window.h>
#include <unistd.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
}

#include "FFmpegMusic.h"
#define LOGE(FORMAT,...) __android_log_print(ANDROID_LOG_ERROR,"LC",FORMAT,##__VA_ARGS__);
