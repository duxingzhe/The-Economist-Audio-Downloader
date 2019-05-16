//
// Created by Luxuan on 2019/5/13.
//

#ifndef NDK_AUDIOPLAYER_H
#define NDK_AUDIOPLAYER_H

#include <assert.h>
#include <string.h>

#include <android/log.h>


#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>

#include <sys/types.h>
#include <ffmpeg_mediaplayer.h>
#include <stdint.h>

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
    uint8_t buffer;
} AudioPlayer;

void createEngine(AudioPlayer **ps);
void createBufferQueueAudioPlayer(AudioPlayer **ps, void *state, int numChannels, int samplesPerSec, int streamType);
void setPlayingAudioPlayer(AudioPlayer **ps, int playstate);
void setVolumeUriAudioPlayer(AudioPlayer **ps, int millibel);
void queueAudioSamples(AudioPlayer **ps, void *state);
int enqueue(AudioPlayer **ps, int16_t *data, int size);
void shutdown(AudioPlayer **ps);

#endif //NDK_AUDIOPLAYER_H
