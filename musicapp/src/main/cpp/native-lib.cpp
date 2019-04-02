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

SLObjectItf engineObject=NULL;
SLEngineItf engienEngine=NULL;

SLObjectItf outputMixObject=NULL;
SLEnvironmentalReverbItf outputMixEnvironmentalReverb=NULL;
SLEnvironmentalReverbSettings settings=SL_I3DL2_ENVIRONMENT_PRESET_DEFAULT;

SLObjectItf audioplayer=NULL;
SLPlayItf slPlayItf=NULL;
SLAndroidSimpleBufferQueueItf slBufferQueueItf=NULL;

size_t buffersize=0;

void *buffer;

void getQueueCallback(SLAndroidSimpleBufferQueueItf slBufferQueueItf, void *context)
{
    buffersize=0;
    getPcm(&buffer, &buffersize);
    if(buffer!=NULL && buffersize!=0)
    {
        (*slBufferQueueItf)->Enqueue(slBufferQueueItf, buffer, buffersize);
    }
}

extern "C"

JNIEXPORT void JNICALL
Java_com_luxuan_musicapp_MusicPlay_play(JNIEnv *env, jobject instance)
{
    createEngine();
    createMixVolume();
    createPlayer();
}