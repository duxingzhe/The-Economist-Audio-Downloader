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
SLEngineItf engineEngine=NULL;

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

void createEngine()
{
    slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
}

void createMixVolume()
{
    (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, 0, 0);
    (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    SLresult sLresult=(*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB, &outputMixEnvironmentalReverb);

    if(SL_RESULT_SUCCESS==sLresult)
    {
        (*outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(outputMixEnvironmentalReverb, &settings);
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

extern "C"
JNIEXPORT void JNICALL
Java_com_luxuan_musicapp_MusicPlay_stop(JNIEnv *env, jobject instance)
{
    realseResource();
}