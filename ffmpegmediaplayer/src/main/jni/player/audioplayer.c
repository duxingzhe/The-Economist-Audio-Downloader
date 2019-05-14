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
        free(player->buffer);
        player->buffer=NULL;
    }

    int len=4096;
    player->buffer=malloc(len);

    is->audio_callback(context, player->buffer, len);
    enqueue(&is->audio_player, (int16_t *)player->buffer, len);
}

void createEngine(AudioPlayer **ps)
{
    AudioPlayer *player=*ps;

    player->buffer=NULL;
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
