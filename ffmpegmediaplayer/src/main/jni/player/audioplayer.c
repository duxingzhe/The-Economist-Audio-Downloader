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
    plyaer->buffer=malloc(len);

    is->audio_callback(context, player->buffer, len);
    enqueue(&is->audio_player, (int16_t *)player->buffer, len);
}