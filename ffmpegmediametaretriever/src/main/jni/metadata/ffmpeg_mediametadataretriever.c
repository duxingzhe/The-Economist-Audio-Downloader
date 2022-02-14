//
// Created by Administrator on 2019/6/1.
//

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/opt.h>
#include <ffmpeg_mediametadataretriever.h>
#include <ffmpeg_utils.h>
#include <libavutil/imgutils.h>

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

    s->scaled_codecCtx->bit_rate=s->video_st->codecpar->bit_rate;
    s->scaled_codecCtx->width=width;
    s->scaled_codecCtx->height=height;
    s->scaled_codecCtx->pix_fmt=TARGET_IMAGE_FORMAT;
    s->scaled_codecCtx->codec_type=AVMEDIA_TYPE_VIDEO;
    s->scaled_codecCtx->time_base.num=s->video_st->codecpar->sample_aspect_ratio.num;
    s->scaled_codecCtx->time_base.den=s->video_st->codecpar->sample_aspect_ratio.den;

    if(!targetCodec||avcodec_open2(s->scaled_codecCtx, targetCodec, NULL)<0)
    {
        printf("avcodec_open2() failed\n");
        return FAILURE;
    }

    s->scaled_sws_ctx=sws_getContext(s->video_st->codecpar->width, s->video_st->codecpar->height,
            s->video_st->codecpar->format, width, height, TARGET_IMAGE_FORMAT, SWS_BILINEAR, NULL,
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

    codecCtx=pFormatCtx->streams[stream_index]->codecpar;

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

            s->codecCtx->bit_rate=s->video_st->codecpar->bit_rate;
            s->codecCtx->width=s->video_st->codecpar->width;
            s->codecCtx->height=s->video_st->codecpar->height;
            s->codecCtx->pix_fmt=TARGET_IMAGE_FORMAT;
            s->codecCtx->codec_type=AVMEDIA_TYPE_VIDEO;
            s->codecCtx->time_base.num=s->video_st->codecpar->sample_aspect_ratio.num;
            s->codecCtx->time_base.den=s->video_st->codecpar->sample_aspect_ratio.den;

            if(!targetCodec||avcodec_open2(s->codecCtx,targetCodec, NULL)<0)
            {
                printf("avcodec_open2() failed\n");
                return FAILURE;
            }

            s->sws_ctx=sws_getContext(s->video_st->codecpar->width, s->video_st->codecpar->height,
                    s->video_st->codecpar->format, s->video_st->codecpar->width, s->video_st->codecpar->height,
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
        if(state->pFormatCtx->streams[i]->codecpar->codec_type==AVMEDIA_TYPE_VIDEO && video_index<0)
        {
            video_index=i;
        }

        if(state->pFormatCtx->streams[i]->codecpar->codec_type==AVMEDIA_TYPE_AUDIO && video_index<0)
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
            av_packet_copy_props(pkt, &state->pFormatCtx->streams[i]->attached_pic);

            got_packet=1;

            if(pkt->stream_index==state->video_stream)
            {
                int codec_id=state->video_st->codecpar->codec_id;
                int pix_fmt=state->video_st->codecpar->format;

                if(!is_supported_format(codec_id, pix_fmt))
                {
                    int got_frame=0;

                    frame=av_frame_alloc();

                    if(!frame)
                    {
                        break;
                    }

                    if(avcodec_send_packet(state->video_st->codecpar, pkt)<=0)
                    {
                        break;
                    }

                    got_frame = avcodec_receive_frame(state->video_st->codecpar, frame);

                    if(got_frame)
                    {
                        AVPacket convertedPkt;
                        av_init_packet(&convertedPkt);
                        convertedPkt.size=0;
                        convertedPkt.data=NULL;

                        convert_image(state, state->video_st->codecpar, frame, &convertedPkt, &got_packet, -1, -1);

                        av_packet_unref(pkt);
                        av_init_packet(pkt);
                        av_packet_ref(pkt, &convertedPkt);

                        av_packet_unref(&convertedPkt);

                        break;
                    }
                }
                else
                {
                    av_packet_unref(pkt);
                    av_init_packet(pkt);
                    av_packet_ref(pkt, &state->pFormatCtx->streams[i]->attached_pic);

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

void convert_image(State *state, AVCodecContext *pCodecCtx, AVFrame *pFrame, AVPacket *avpkt, int *got_packet_ptr, int width, int height)
{
    AVCodecContext *codecCtx;
    struct SwsContext *scalerCtx;
    AVFrame *frame;

    *got_packet_ptr=0;

    if(width!=-1&&height!=-1)
    {
        if(state->scaled_codecCtx==NULL || state->scaled_sws_ctx==NULL)
        {
            get_scaled_context(state, pCodecCtx, width, height);
        }

        codecCtx=state->scaled_codecCtx;
        scalerCtx=state->scaled_sws_ctx;
    }
    else
    {
        codecCtx=state->codecCtx;
        scalerCtx=state->sws_ctx;
    }

    if(width==-1)
    {
        width=pCodecCtx->width;
    }

    if(height==-1)
    {
        height=pCodecCtx->height;
    }

    frame=av_frame_alloc();

    int numBytes=av_image_get_buffer_size(TARGET_IMAGE_FORMAT, codecCtx->width, codecCtx->height,1 );
    void* buffer=(uint8_t *)av_malloc(numBytes*sizeof(uint8_t));

    frame->format=TARGET_IMAGE_FORMAT;
    frame->width=codecCtx->width;
    frame->height=codecCtx->height;

    av_image_fill_arrays(frame->data, frame->linesize, buffer, TARGET_IMAGE_FORMAT, codecCtx->width, codecCtx->height, 1);

    sws_scale(scalerCtx, (const uint8_t* const *)pFrame->data, pFrame->linesize, 0, pFrame->height, frame->data,
            frame->linesize);

    int ret=avcodec_send_frame(codecCtx, frame);
    *got_packet_ptr = avcodec_receive_packet(codecCtx, avpkt);

    if(ret>=0&& state->native_window)
    {
        ANativeWindow_setBuffersGeometry(state->native_window, width, height, WINDOW_FORMAT_RGBA_8888);

        ANativeWindow_Buffer windowBuffer;

        if(ANativeWindow_lock(state->native_window, &windowBuffer, NULL)==0)
        {
            int h=0;

            for(h=0;h<height;h++)
            {
                memcpy(windowBuffer.bits+h*windowBuffer.stride*4, buffer+h*frame->linesize[0], width*4);
            }

            ANativeWindow_unlockAndPost(state->native_window);
        }
    }

    if(ret<0)
    {
        *got_packet_ptr=0;
    }

    av_frame_free(&frame);

    if(buffer)
    {
        free(buffer);
    }

    if(ret<0||!*got_packet_ptr)
    {
        av_packet_unref(avpkt);
    }
}

void decode_frame(State *state, AVPacket *pkt, int *got_frame, int64_t desired_frame_number, int width, int height)
{
    AVFrame *frame=av_frame_alloc();

    *got_frame=0;

    if(!frame)
    {
        return;
    }

    while(av_read_frame(state->pFormatCtx, pkt)>=0)
    {
        if(pkt->stream_index==state->video_stream)
        {
            int codec_id=state->video_st->codecpar->codec_id;
            int pix_fmt=state->video_st->codecpar->format;

            if(!is_supported_format(codec_id, pix_fmt))
            {
                *got_frame=0;

                if(avcodec_send_packet(state->video_st->codecpar,  pkt)<=0)
                {
                    *got_frame=0;
                    break;
                }

                *got_frame = avcodec_receive_frame(state->video_st->codecpar, frame);

                if(*got_frame)
                {
                    if(desired_frame_number==-1|| (desired_frame_number!=-1&&frame->pts>=desired_frame_number))
                    {
                        if(pkt->data)
                        {
                            av_packet_unref(pkt);
                        }
                        av_init_packet(pkt);
                        convert_image(state, state->video_st->codecpar, frame, pkt, got_frame, width, height);
                        break;
                    }
                }
            }
            else
            {
                *got_frame=1;
                break;
            }
        }
    }

    av_frame_free(&frame);
}

int get_frame_at_time(State **ps, int64_t timeUs, int option, AVPacket *pkt)
{
    return get_scaled_frame_at_time(ps, timeUs, option, pkt, -1, -1);
}

int get_scaled_frame_at_time(State **ps, int64_t timeUs, int option, AVPacket *pkt, int width, int height)
{
    printf("get_frame_at_time\n");
    int got_packet=0;
    int64_t desired_frame_number=-1;

    State *state=*ps;
    Options opt=option;

    if(!state||!state->pFormatCtx||state->video_stream<0)
    {
        return FAILURE;
    }

    if(timeUs>-1)
    {
        int stream_index=state->video_stream;
        int64_t seek_time=av_rescale_q(timeUs, AV_TIME_BASE_Q, state->pFormatCtx->streams[stream_index]->time_base);
        int64_t seek_stream_duration=state->pFormatCtx->streams[stream_index]->duration;

        int flags=0;
        int ret=-1;

        if(seek_stream_duration>0&&seek_time>seek_stream_duration)
        {
            seek_time=seek_stream_duration;
        }

        if(seek_time<0)
        {
            return FAILURE;
        }

        if(opt==OPTION_CLOSET)
        {
            desired_frame_number=seek_time;
            flags=AVSEEK_FLAG_BACKWARD;
        }
        else if(opt==OPTION_CLOSET_SYNC)
        {
            flags=0;
        }
        else if(opt==OPTION_NEXT_SYNC)
        {
            flags=0;
        }
        else if(opt==OPTION_PREVIOUS_SYNC)
        {
            flags=AVSEEK_FLAG_BACKWARD;
        }

        ret=av_seek_frame(state->pFormatCtx, stream_index, seek_time, flags);

        if(ret<0)
        {
            return FAILURE;
        }
        else
        {
            if(state->audio_stream>=0)
            {
                avcodec_flush_buffers(state->audio_st->codecpar);
            }

            if(state->video_stream>=0)
            {
                avcodec_flush_buffers(state->video_st->codecpar);
            }
        }
    }

    decode_frame(state, pkt, &got_packet, desired_frame_number, width, height);

    if(got_packet)
    {

    }

    if(got_packet)
    {
        return SUCCESS;
    }
    else
    {
        return FAILURE;
    }
}

int set_native_window(State **ps, ANativeWindow *native_window)
{
    printf("set_native_window\n");

    State *state= *ps;

    if(native_window==NULL)
    {
        return FAILURE;
    }

    if(!state)
    {
        init(&state);
    }

    state->native_window=native_window;

    *ps=state;

    return SUCCESS;
}

void release(State **ps)
{
    printf("release\n");

    State *state=*ps;

    if(state)
    {
        if(state->audio_st&&state->audio_st->codecpar)
        {
            avcodec_close(state->audio_st->codecpar);
        }

        if(state->video_st&&state->video_st->codecpar)
        {
            avcodec_close(state->video_st->codecpar);
        }

        if(state->pFormatCtx)
        {
            avformat_close_input(&state->pFormatCtx);
        }

        if(state->fd!=-1)
        {
            close(state->fd);
        }

        if(state->sws_ctx)
        {
            sws_freeContext(state->sws_ctx);
            state->sws_ctx=NULL;
        }

        if(state->codecCtx)
        {
            avcodec_close(state->codecCtx);
            av_free(state->codecCtx);
        }

        if(state->sws_ctx)
        {
            sws_freeContext(state->sws_ctx);
        }

        if(state->scaled_codecCtx)
        {
            avcodec_close(state->scaled_codecCtx);
            av_free(state->scaled_codecCtx);
        }

        if(state->scaled_sws_ctx)
        {
            sws_freeContext(state->scaled_sws_ctx);
        }

        if(state->native_window!=NULL)
        {
            ANativeWindow_release(state->native_window);
            state->native_window=NULL;
        }

        av_freep(&state);
        ps=NULL;
    }
}