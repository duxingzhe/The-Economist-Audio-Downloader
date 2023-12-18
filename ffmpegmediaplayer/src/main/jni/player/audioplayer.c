//
// Created by Luxuan on 2019/5/13.
//

#include "audioplayer.h"

void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
    VideoState *is=(VideoState *)context;

    AudioPlayer *player=&is->audio_player;

    if(player->buffer!=NULL)
    {
        free(&player->buffer);
        player->buffer= (uint8_t) NULL;
    }

    int len=4096;
    player->buffer= (uint8_t) malloc(len);

    is->audio_callback(context, &player->buffer, len);
    enqueue(&is->audio_player, (int16_t *)player->buffer, len);
}

void createEngine(AudioPlayer **ps)
{
    AudioPlayer *player=*ps;

    player->buffer= (uint8_t) NULL;
    SLresult result;

    result=slCreateEngine(&player->engineObject, 0, NULL, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS==result);
    (void)result;

    result=(*player->engineObject)->Realize(player->engineObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS==result);
    (void)result;

    const SLInterfaceID ids[1]={SL_IID_ENVIRONMENTALREVERB};
    const SLboolean req[1]={SL_BOOLEAN_FALSE};
    result=(*player->engineEngine)->CreateOutputMix(player->engineEngine, &player->outputMixObject, 0, ids, req);
    assert(SL_RESULT_SUCCESS==result);
    (void)result;

    result=(*player->outputMixObject)->Realize(player->outputMixObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS==result);
    (void)result;
}

void createBufferQueueAudioPlayer(AudioPlayer **ps, void *state, int numChannels, int samplesPerSec, int streamType)
{
    AudioPlayer *player=*ps;

    SLuint32 channelMask=0;

    if(numChannels==2)
    {
        channelMask=SL_SPEAKER_FRONT_LEFT|SL_SPEAKER_FRONT_RIGHT;
    }
    else if(numChannels==3)
    {
        channelMask=SL_SPEAKER_FRONT_CENTER;
    }
    else
    {
        channelMask=SL_SPEAKER_FRONT_CENTER;
    }

    SLresult result;

    SLDataLocator_BufferQueue loc_bufq={SL_DATALOCATOR_BUFFERQUEUE, BUFFER_COUNT};
    SLDataFormat_PCM format_pcm={SL_DATAFORMAT_PCM, numChannels, samplesPerSec*100,
                                 SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
                                 channelMask, SL_BYTEORDER_LITTLEENDIAN};
    SLDataSource audioSrc={&loc_bufq, &format_pcm};

    SLDataLocator_OutputMix loc_outmix={SL_DATALOCATOR_OUTPUTMIX, player->outputMixObject};
    SLDataSink audioSnk={&loc_outmix, NULL};

    const SLInterfaceID ids[4]={SL_IID_BUFFERQUEUE, SL_IID_EFFECTSEND,
            SL_IID_VOLUME, SL_IID_ANDROIDCONFIGURATION};
    const SLboolean req[4]={SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE,
                            SL_BOOLEAN_TRUE};
    result=(*player->engineEngine)->CreateAudioPlayer(player->engineEngine, &player->bqPlayerObject, &audioSrc, &audioSnk,
            4,ids, req);
    assert(SL_RESULT_SUCCESS==result);
    (void)result;

    SLAndroidConfigurationItf playerConfig;
    result=(*player->bqPlayerObject)->GetInterface(player->bqPlayerObject, SL_IID_ANDROIDCONFIGURATION, (void *)&playerConfig);
    assert(SL_RESULT_SUCCESS==result);
    (void)result;

    result=(*playerConfig)->SetConfiguration(playerConfig,SL_ANDROID_KEY_STREAM_TYPE, &streamType, sizeof(SLint32));
    assert(SL_RESULT_SUCCESS==result);
    (void)result;

    result=(*player->bqPlayerObject)->Realize(player->bqPlayerObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS==result);
    (void)result;

    result=(*player->bqPlayerObject)->GetInterface(player->bqPlayerObject, SL_IID_PLAY, &player->bqPlayerPlay);
    assert(SL_RESULT_SUCCESS==result);
    (void)result;

    result=(*player->bqPlayerObject)->GetInterface(player->bqPlayerObject, SL_IID_BUFFERQUEUE, &player->bqPlayerBufferQueue);
    assert(SL_RESULT_SUCCESS==result);
    (void)result;

#if 0
    result=(*player->bqPlayerObject)->GetInterface(player->bqPlayerObject, SL_IID_MUTESOLO, &player->bqPlayerMuteSolo);
    assert(SL_RESULT_SUCCESS==result);
    (void)result;
#endif

    result=(*player->bqPlayerObject)->GetInterface(player->bqPlayerObject, SL_IID_VOLUME, &player->bqPlayerVolume);
    assert(SL_RESULT_SUCCESS==result);
    (void)result;
}

void setPlayingAudioPlayer(AudioPlayer **ps, int playstate)
{
    AudioPlayer *player=*ps;

    SLresult result;

    int state=0;

    if(playstate==0)
    {
        state=SL_PLAYSTATE_PLAYING;
    }
    else if(playstate==1)
    {
        state=SL_PLAYSTATE_PAUSED;
    }
    else
    {
        state=SL_PLAYSTATE_STOPPED;
    }

    if(NULL!=player->bqPlayerPlay)
    {
        result=(*player->bqPlayerPlay)->SetPlayState(player->bqPlayerPlay, state);
        assert(SL_RESULT_SUCCESS==result);
        (void)result;
    }
}

SLVolumeItf getVolume(AudioPlayer *player)
{
    return player->bqPlayerVolume;
}

void setVolumeUriAudioPlayer(AudioPlayer **ps, int millibel)
{
    AudioPlayer *player=*ps;

    SLresult result;
    SLVolumeItf volumeItf=getVolume(player);
    if(NULL!=volumeItf)
    {
        SLmillibel MinVolume=SL_MILLIBEL_MIN;
        SLmillibel MaxVolume=SL_MILLIBEL_MIN;

        (*volumeItf)->GetMaxVolumeLevel(volumeItf, &MaxVolume);

        SLmillibel volume=MinVolume+(SLmillibel)(((float)(MaxVolume-MinVolume))*millibel);

        result=(*volumeItf)->SetVolumeLevel(volumeItf, volume);
        assert(SL_RESULT_SUCCESS==result);
        (void)result;
    }
}

void queueAudioSamples(AudioPlayer **ps, void *state)
{
    AudioPlayer *player=*ps;

    bqPlayerCallback(NULL, state);
}

int enqueue(AudioPlayer **ps, int16_t *data, int size)
{
    AudioPlayer *player=*ps;

    SLresult result;
    result=(*player->bqPlayerBufferQueue)->Enqueue(player->bqPlayerBufferQueue, data, size);
    if(SL_RESULT_SUCCESS!=result)
    {
        return 0;
    }

    return 0;
}

void shutdown(AudioPlayer **ps)
{
    AudioPlayer *player=*ps;

    if(player->bqPlayerObject!=NULL)
    {
        (*player->bqPlayerObject)->Destroy(player->bqPlayerObject);
        player->bqPlayerObject=NULL;
        player->bqPlayerPlay=NULL;
        player->bqPlayerBufferQueue=NULL;
        player->bqPlayerEffectSend=NULL;
        player->bqPlayerMuteSolo=NULL;
        player->bqPlayerVolume=NULL;
    }

    if(player->outputMixObject!=NULL)
    {
        (*player->outputMixObject)->Destroy(player->outputMixObject);
        player->outputMixObject=NULL;
    }

    if(player->engineObject!=NULL)
    {
        (*player->engineObject)->Destroy(player->engineObject);
        player->engineObject=NULL;
        player->engineEngine=NULL;
    }

    if(player->buffer!=NULL)
    {
        free(&player->buffer);
        player->buffer= (uint8_t) NULL;
    }

}