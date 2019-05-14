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
#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR, "LC", FORMAT, ##__VA_ARGS__);

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

void createPlayer()
{
    int rate;
    int channels;
    createFFmpeg(&rate, &channels);
    LOGE("RATE %d", rate);
    LOGE("channels %d", channels);

    /*
    *
    typedef struct SLDataLocator_AndroidBufferQueue_ {
        SLuint32    locatorType;//缓冲区队列类型
        SLuint32    numBuffers;//buffer位数
    } */

    SLDataLocator_AndroidBufferQueue android_queue={SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};

    /**
    typedef struct SLDataFormat_PCM_ {
        SLuint32 		formatType;  pcm
        SLuint32 		numChannels;  通道数
        SLuint32 		samplesPerSec;  采样率
        SLuint32 		bitsPerSample;  采样位数
        SLuint32 		containerSize;  包含位数
        SLuint32 		channelMask;     立体声
        SLuint32		endianness;    end标志位
    } SLDataFormat_PCM;
     */
     SLDataFormat_PCM pcm={SL_DATAFORMAT_PCM, (SLuint32)channels, (SLuint32)rate*1000, SL_PCMSAMPLEFORMAT_FIXED_16,
                           SL_PCMSAMPLEFORMAT_FIXED_16, SL_SPEAKER_FRONT_LEFT|SL_SPEAKER_FRONT_RIGHT,
                           SL_BYTEORDER_LITTLEENDIAN};
    /*
    * typedef struct SLDataSource_ {
           void *pLocator;//缓冲区队列
           void *pFormat;//数据样式,配置信息
       } SLDataSource;
    * */
    SLDataSource dataSource={&android_queue, &pcm};

    SLDataLocator_OutputMix slDataLocator_outputMix={SL_DATALOCATOR_OUTPUTMIX, outputMixObject};

    SLDataSink slDataSink={&slDataLocator_outputMix, NULL};

    const SLInterfaceID ids[3]={SL_IID_BUFFERQUEUE, SL_IID_EFFECTSEND, SL_IID_VOLUME};
    const SLboolean req[3]={SL_BOOLEAN_FALSE, SL_BOOLEAN_FALSE, SL_BOOLEAN_FALSE};

    /*
     * SLresult (*CreateAudioPlayer) (
		SLEngineItf self,
		SLObjectItf * pPlayer,
		SLDataSource *pAudioSrc,//数据设置
		SLDataSink *pAudioSnk,//关联混音器
		SLuint32 numInterfaces,
		const SLInterfaceID * pInterfaceIds,
		const SLboolean * pInterfaceRequired
	);
     * */
    LOGE("执行到此处")
    (*engineEngine)->CreateAudioPlayer(engineEngine, &audioplayer, &dataSource, &slDataSink, 3, ids, req);
    (*audioplayer)->Realize(audioplayer, SL_BOOLEAN_FALSE);
    LOGE("执行到此处2")
    (*audioplayer)->GetInterface(audioplayer, SL_IID_PLAY, &slPlayItf);
    (*audioplayer)->GetInterface(audioplayer, SL_IID_BUFFERQUEUE, &slBufferQueueItf);
    (*slBufferQueueItf)->RegisterCallback(slBufferQueueItf, getQueueCallback, NULL);
    (*slPlayItf)->SetPlayState(slPlayItf, SL_PLAYSTATE_PLAYING);

    getQueueCallback(slBufferQueueItf, NULL);
}

void realseResource()
{
    if(audioplayer!=NULL)
    {
        (*audioplayer)->Destroy(audioplayer);
        audioplayer=NULL;
        slBufferQueueItf=NULL;
        slPlayItf=NULL;
    }
    if(outputMixObject!=NULL)
    {
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject=NULL;
        outputMixEnvironmentalReverb=NULL;
    }

    if(engineObject!=NULL)
    {
        (*engineObject)->Destroy(engineObject);
        engineObject=NULL;
        engineEngine=NULL;
    }
    realseFFmpeg();
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