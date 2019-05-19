//
// Created by Administrator on 2019/5/16.
//

#include <ffmpeg_mediaplayer.h>

static int one=0;
static int two=0;

void packet_queue_init(PacketQueue *q)
{
    memset(q, 0, sizeof(PacketQueue));
    q->initialized=1;
    q->mutex=SDL_CreateMutex();
    q->cond=SDL_CreateCond();
}

int packet_queue_put(VideoState *is, PacketQueue *q, AVPacket *pkt)
{
    AVPacketList *pkt1;
    if(pkt!=&is->flush_pkt && av_dup_packet(pkt)<0)
    {
        return -1;
    }
    pkt1=av_malloc(sizeof(AVPacketList));
    if(!pkt1)
        return -1;
    pkt1->pkt=*pkt;
    pkt1->next=NULL;

    SDL_LockMutex(q->mutex);

    if(!q->last_pkt)
        q->first_pkt=pkt1;
    else
        q->last_pkt->next=pkt1;
    q->last_pkt=pkt1;
    q->nb_packets++;
    q->size+=pkt1->pkt.size;
    SDL_CondSignal(q->cond);

    SDL_UnlockMutex(q->mutex);
    return 0;
}

static int packet_queue_get(VideoState *is, PacketQueue *q, AVPacket *pkt, int block)
{
    AVPacketList *pkt1;
    int ret;

    SDL_LockMutex(q->mutex);

    for( ; ; )
    {
        if(is->quit)
        {
            ret=-1;
            break;
        }

        pkt1=q->first_pkt;
        if(pkt1)
        {
            q->first_pkt=pkt1->next;
            if(!q->first_pkt)
                q->last_pkt = NULL;
            q->nb_packets--;
            q->size-=pkt1->pkt.size;
            *pkt=pkt1->pkt;
            av_free(pkt1);
            ret=1;
            break;
        }
        else if(!block)
        {
            ret=0;
            break;
        }
        else
        {
            SDL_CondWait(q->cond, q->mutex);
        }
    }
    SDL_UnlockMutex(q->mutex);
    return ret;
}

static void packet_queue_flush(PacketQueue *q)
{
    AVPacketList *pkt, *pkt1;

    SDL_LockMutex(q->mutex);
    for(pkt=q->first_pkt;pkt!=NULL;pkt=pkt1)
    {
        pkt1=pkt->next;
        av_packet_unref(&pkt->pkt);
        av_freep(&pkt);
    }
    q->last_pkt=NULL;
    q->first_pkt=NULL;
    q->nb_packets=0;
    q->size=0;
    SDL_UnlockMutex(q->mutex);
}

double get_audio_clock(VideoState *is)
{
    double pts;
    int hw_buf_size, bytes_per_sec, n;

    pts=is->audio_clock;
    hw_buf_size=is->audio_buf_size-is->audio_buf_index;
    bytes_per_sec=0;
    n=is->audio_st->codec->channels*2;
    if(is->audio_st)
    {
        bytes_per_sec=is->audio_st->codec->sample_rate*n;
    }
    if(bytes_per_sec)
    {
        pts-=(double)hw_buf_size/bytes_per_sec;
    }
    return pts;
}

double get_video_clock(VideoState *is)
{
    double delta;

    delta=(av_gettime()-is->video_current_pts_time)/1000000.0;
    return is->video_current_pts+delta;
}

double get_external_clock(VideoState *is)
{
    return av_gettime()/1000000.0;
}

double get_master_clock(VideoState *is)
{
    if(is->av_sync_type==AV_SYNC_VIDEO_MASTER)
    {
        return get_video_clock(is);
    }
    else if(is->av_sync_type==AV_SYNC_AUDIO_MASTER)
    {
        return get_audio_clock(is);
    }
    else
    {
        return get_external_clock(is);
    }
}

int synchronize_audio(VideoState *is, short *samples, int samples_size, double pts)
{
    int n;
    double ref_clock;

    n=2*is->audio_st->codec->channels;

    if(is->av_sync_type!=AV_SYNC_AUDIO_MASTER)
    {
        double diff, avg_diff;
        int wanted_size, min_size, max_size;

        ref_clock=get_master_clock(is);
        diff=get_audio_clock(is)-ref_clock;

        if(diff<AV_NOSYNC_THRESHOLD)
        {
            is->audio_diff_cum=diff+is->audio_diff_avg_coef*is->audio_diff_cum;
            if(is->audio_diff_avg_count<AUDIO_DIFF_AVG_NB)
            {
                is->audio_diff_avg_count++;
            }
            else
            {
                avg_diff=is->audio_diff_cum*(1.0-is->audio_diff_avg_coef);
                if(fabs(avg_diff)>=is->audio_diff_threashold)
                {
                    wanted_size=samples_size+((int)(diff*is->audio_st->codec->sample_rate)*n);
                    min_size=samples_size*((100-SAMPLE_CORRECTION_PERCENT_MAX)/100);
                    max_size=samples_size*((100+SAMPLE_CORRECTION_PERCENT_MAX)/100);
                    if(wanted_size<min_size)
                    {
                        wanted_size=min_size;
                    }
                    else if(wanted_size>max_size)
                    {
                        wanted_size=max_size;
                    }

                    if(wanted_size<samples_size)
                    {
                        samples_size=wanted_size;
                    }
                    else if(wanted_size>samples_size)
                    {
                        uint8_t *samples_end, *q;
                        int nb;

                        nb=(samples_size-wanted_size);
                        samples_end=(uint8_t *)samples+samples_size-n;
                        q=samples_end+n;
                        while(nb>0)
                        {
                            memcpy(q, samples_end, n);
                            q+=n;
                            nb-=n;
                        }
                        samples_size=wanted_size;
                    }
                }
            }
        }
        else
        {
            is->audio_diff_avg_count=0;
            is->audio_diff_cum=0;
        }
    }

    return samples_size;
}
