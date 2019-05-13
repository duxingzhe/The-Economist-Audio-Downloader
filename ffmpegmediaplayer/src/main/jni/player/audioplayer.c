//
// Created by Luxuan on 2019/5/13.
//

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>

#include <sys/types.h>
#include <ffmpeg_mediaplayer.h>
#include <stdin.h>

static const int BUFFER_COUNT=2;

static const SLEnvironmentalReverbSettings reverbSettings=
        SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;

typedef struct AudioPlayer
{
    SLObjectItf engineObject;
    SLEngineItf engineEngine;

    SLObjectItf outputMixObject;

    SLObjectItf bqPlayerObject;
    SLPlayItf bqPlayerPlay;
    SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;
    SLEffectSendItf bqPlayerEffectSend;
    SLMuteSoloItf bqPlayerMuteSolo;
    SLVolumeItf bqPlayerVolume;

    void (*bqPlayerCallback) (SLAndroidSimpleBufferQueueItf, void*);
    void (*audio_callback) (void *userdata, uint8_t *stream, int len);
} AudioPlayer;