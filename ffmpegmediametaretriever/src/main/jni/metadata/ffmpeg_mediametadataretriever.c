//
// Created by Administrator on 2019/6/1.
//

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/opt.h>
#include <ffmpeg_mediametadataretriever.h>
#include <ffmpeg_utils.h>

#include <stdio.h>
#include <unistd.h>

#include <android/log.h>

const int TARGET_IMAGE_FORMAT=AV_PIX_FMT_RGBA;
const int TARGET_IMAGE_CODEC=AV_CODEC_ID_PNG;

void convert_image(State *state, AVCodecContext *pCodecCtx, AVFrame *pFrame, AVPacket *avpkt, int *got_packet_ptr, int width, int height);

int is_supported_format(int codec_id, int pix_fmt)
{
    if((codec_id==AV_CODEC_ID_PNG || codec_id==AV_CODEC_ID_MJPEG ||
        codec_id==AV_CODEC_ID_BMP )&&(pix_fmt==AV_PIX_FMT_RGBA))
    {
        return 1;
    }

    return 0;
}

int get_scaled_context(State *s, AVCodecContext *pCodecContext, int width, int height)
{
    AVCodec *targetCodec=avcodec_find_encoder(TARGET_IMAGE_CODEC);
    if(!targetCodec)
    {
        printf("avcodec_find_decoder() failed to find encoder\n");
        return FAILURE;
    }

    s->scaled_codecCtx=avcodec_alloc_context3(targetCodec);
    if(!s->scaled_codecCtx)
    {
        printf("avcodec_alloc_context3 failed\n");
        return FAILURE;
    }

    s->scaled_codecCtx->bit_rate=s->video_st->codec->bit_rate;
    s->scaled_codecCtx->width=width;
    s->scaled_codecCtx->height=height;
    s->scaled_codecCtx->pix_fmt=TARGET_IMAGE_FORMAT;
    s->scaled_codecCtx->codec_type=AVMEDIA_TYPE_VIDEO;
    s->scaled_codecCtx->time_base.num=s->video_st->codec->time_base.num;
    s->scaled_codecCtx->time_base.den=s->video_st->codec->time_base.den;

    if(!targetCodec||avcodec_open2(s->scaled_codecCtx, targetCodec, NULL)<0)
    {
        printf("avcodec_open2() failed\n");
        return FAILURE;
    }

    s->scaled_sws_ctx=sws_getContext(s->video_st->codec->width, s->video_st->codec->height,
            s->video_st->codec->pix_fmt, width, height, TARGET_IMAGE_FORMAT, SWS_BILINEAR, NULL,
            NULL, NULL);

    return SUCCESS;
}

int stream_component_open(State *s, int stream_index)
{
    AVFormatContext *pFormatCtx=s->pFormatCtx;
    AVCodecContext *codecCtx;
    AVCodec *codec;

    if(stream_index<0||stream_index>=pFormatCtx->nb_streams)
    {
        return FAILURE;
    }

    codecCtx=pFormatCtx->streams[stream_index]->codec;

    const AVCodecDescriptor *codesc=avcodec_descriptor_get(codecCtx->codec_id);
    if(codesc)
    {
        printf("avcodec_find_decoder %s\n", codesc->name);
    }

    codec=avcodec_find_decoder(codecCtx->codec_id);

    if(codec==NULL)
    {
        printf("avcodec_find_decoder() failed to find audio decoder\n");
        return FAILURE;
    }

    if(!codec||(avcodec_open2(codecCtx, codec, NULL)<0))
    {
        printf("avcodec_open2() failed\n");
        return FAILURE;
    }

    switch(codecCtx->codec_type)
    {
        case AVMEDIA_TYPE_AUDIO:
            s->audio_stream=stream_index;
            s->audio_st=pFormatCtx->streams[stream_index];
            break;
        case AVMEDIA_TYPE_VIDEO:
            s->video_stream=stream_index;
            s->video_st=pFormatCtx->streams[stream_index];

            AVCodec *targetCodec=avcodec_find_encoder(TARGET_IMAGE_CODEC);
            if(!targetCodec)
            {
                printf("avcodec_find_decoder() failed to find encoder\n");
                return FAILURE;
            }

            s->codecCtx->bit_rate=s->video_st->codec->bit_rate;
            s->codecCtx->width=s->video_st->codec->width;
            s->codecCtx->height=s->video_st->codec->height;
            s->codecCtx->pix_fmt=TARGET_IMAGE_FORMAT;
            s->codecCtx->codec_type=AVMEDIA_TYPE_VIDEO;
            s->codecCtx->time_base.num=s->video_st->codec->time_base.num;
            s->codecCtx->time_base.den=s->video_st->codec->time_base.den;

            if(!targetCodec||avcodec_open2(s->codecCtx,targetCodec, NULL)<0)
            {
                printf("avcodec_open2() failed\n");
                return FAILURE;
            }

            s->sws_ctx=sws_getContext(s->video_st->codec->width, s->video_st->codec->height,
                    s->video_st->codec->pix_fmt, s->video_st->codec->width, s->video_st->codec->height,
                    TARGET_IMAGE_FORMAT, SWS_BILINEAR, NULL, NULL, NULL);
            break;
        default:
            break;
    }

    return SUCCESS;
}

int set_data_source_l(State **ps, const char* path)
{
    printf("set_data_source\n");
    int audio_index=-1;
    int video_index=-1;
    int i;

    State *state=*ps;

    printf("Path: %s\n", path);

    AVDictionary *options=NULL;
    av_dict_set(&options, "icy", "1", 0);
    av_dict_set(&options, "user-agent", "FFmpegMediaMetadataRetriever", 0);

    if(state->headers)
    {
        av_dict_set(&options, "headers", state->headers, 0);
    }

    if(state->offset>0)
    {
        state->pFormatCtx=avformat_alloc_context();
        state->pFormatCtx->skip_initial_bytes=state->offset;
    }

    if(avformat_open_input(&state->pFormatCtx, path, NULL, &options)!=0)
    {
        printf("Metadata could not be retrieved\n");
        *ps=NULL;
        return FAILURE;
    }

    if(avformat_find_stream_info(state->pFormatCtx, NULL)<0)
    {
        printf("Metadata could not be retrieved\n");
        *ps=NULL;
        return FAILURE;
    }

    set_duration(state->pFormatCtx);

    set_shoutcast_metadata(state->pFormatCtx);

    for(i=0;i<state->pFormatCtx->nb_streams;i++)
    {
        if(state->pFormatCtx->streams[i]->codec->codec_type==AVMEDIA_TYPE_VIDEO && video_index<0)
        {
            video_index=i;
        }

        if(state->pFormatCtx->streams[i]->codec->codec_type==AVMEDIA_TYPE_AUDIO && video_index<0)
        {
            audio_index=i;
        }

        set_codec(state->pFormatCtx, i);
    }

    if(audio_index>=0)
    {
        stream_component_open(state, audio_index);
    }

    if(video_index>=0)
    {
        stream_component_open(state, video_index);
    }

    *ps=state;

    return SUCCESS;
}

void init(State **ps)
{
    State *state=*ps;

    if(state&&state->pFormatCtx)
    {
        avformat_close_input(&state->pFormatCtx);
    }

    if(state&&state->fd!=-1)
    {
        close(state->fd);
    }

    if(!state)
    {
        state=av_malloc(sizeof(State));
    }

    state->pFormatCtx=NULL;
    state->audio_stream=-1;
    state->video_stream=-1;
    state->audio_st=NULL;
    state->video_st=NULL;
    state->fd=-1;
    state->offset=0;
    state->headers=NULL;

    *ps=state;
}

int set_data_source_uri(State **ps, const char* path, const char* headers)
{
    State *state=*ps;
    ANativeWindow *native_window=NULL;

    if(state&& state->native_window)
    {
        native_window=state->native_window;
    }

    init(&state);

    state->native_window=native_window;
    state->headers=headers;

    *ps=state;

    return set_data_source_l(ps, path);
}

int set_data_source_fd(State **ps, int fd, int64_t offset, int64_t length)
{
    char path[256]="";

    State *state=*ps;

    ANativeWindow *native_window=NULL;

    if(state&&state->native_window)
    {
        native_window=state->native_window;
    }

    init(&state);

    state->native_window=native_window;

    int myfd=dup(fd);
    char str[20];

    sprintf(str, "pipe:%d", myfd);
    strcat(path, str);

    state->fd=myfd;
    state->offset=offset;

    *ps=state;

    return set_data_source_l(ps, path);
}

const char* extract_metadata(State **ps, const char* key)
{
    printf("extract_metadata\n");
    char* value=NULL;

    State *state=*ps;

    if(!state||!state->pFormatCtx)
    {
        return value;
    }

    return extract_metadata_internal(state->pFormatCtx, state->audio_st, state->video_st, key);
}

const char* extract_metadata_from_chapter(State **ps, const char *key, int chapter)
{
    printf("extract_metadata_from_chapter\n");
    char* value=NULL;
    State *state=*ps;

    if(!state||!state->pFormatCtx||state->pFormatCtx->nb_chapters<=0)
    {
        return value;
    }

    if(chapter<0||chapter>=state->pFormatCtx->nb_chapters)
    {
        return value;
    }

    return extract_metadata_from_chapter_internal(state->pFormatCtx, state->audio_st, state->video_st, key, chapter);
}

int get_metadata(State **ps, AVDictionary **metadata)
{
    printf("et_metadata\n");

    State *state= *ps;

    if(!state||!state->pFormatCtx)
    {
        return FAILURE;
    }

    get_metadata_internal(state->pFormatCtx, metadata);

    return SUCCESS;
}

int get_embedded_picture(State **ps, AVPacket *pkt)
{
    printf("get_embedded_picture\n");
    int i=0;
    int got_packet=0;
    AVFrame *frame=NULL;

    State *state=*ps;

    if(!state|| !state->pFormatCtx)
    {
        return FAILURE;
    }

    for(i=0;i<state->pFormatCtx->nb_streams;i++)
    {
        if(state->pFormatCtx->streams[i]->disposition& AV_DISPOSITION_ATTACHED_PIC)
        {
            printf("Found album art\n");
            if(pkt)
            {
                av_packet_unref(pkt);
                av_init_packet(pkt);
            }
            av_copy_packet(pkt, &state->pFormatCtx->streams[i]->attached_pic);

            got_packet=1;

            if(pkt->stream_index==state->video_stream)
            {
                int codec_id=state->video_st->codec->codec_id;
                int pix_fmt=state->video_st->codec->pix_fmt;

                if(!is_supported_format(codec_id, pix_fmt))
                {
                    int got_frame=0;

                    frame=av_frame_alloc();

                    if(!frame)
                    {
                        break;
                    }

                    if(avcodec_decode_video2(state->video_st->codec, frame, &got_frame, pkt)<=0)
                    {
                        break;
                    }

                    if(got_frame)
                    {
                        AVPacket convertedPkt;
                        av_init_packet(&convertedPkt);
                        convertedPkt.size=0;
                        convertedPkt.data=NULL;

                        convert_image(state, state->video_st->codec, frame, &convertedPkt, &got_packet, -1, -1);

                        av_packet_unref(pkt);
                        av_init_packet(pkt);
                        av_copy_packet(pkt, &convertedPkt);

                        av_packet_unref(&convertedPkt);

                        break;
                    }
                }
                else
                {
                    av_packet_unref(pkt);
                    av_init_packet(pkt);
                    av_copy_packet(pkt, &state->pFormatCtx->streams[i]->attached_pic);

                    got_packet=1;
                    break;
                }
            }
        }
    }

    av_frame_free(&frame);

    if(got_packet)
    {
        return SUCCESS;
    }
    else
    {
        FAILURE;
    }
}