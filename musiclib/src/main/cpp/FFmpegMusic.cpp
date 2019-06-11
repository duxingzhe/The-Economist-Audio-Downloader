#include "FFmpegMusic.h"

AVFormatContext *formatContext;
AVCodecContext *codecContext;
AVCodec *codex;
AVPacket *packet;
AVFrame *frame;
SwrContext *swrContext;
uint8_t *out_buffer;
int out_channel_nb;
int audio_stream_index=-1;

int createFFmpeg(int *rate, int *channel)
{
    av_register_all();
    char *input="/sdcard/input.mp3";
    formatContext=avformat_alloc_context();
    LOGE("Lujng %s", input);
    LOGE("xx %p",formatContext);
    int error;
    char buf[]="";
    if(error=avformat_open_input(&formatContext, input, NULL,NULL)<0)
    {
        av_strerror(error,buf,1024);
        LOGE("Couldn't open file %s: %d(%s)",input, error, buf);
        LOGE("打开视频失败");
    }
    if(avformat_find_stream_info(formatContext, NULL)<0)
    {
        LOGE("%s", "获取视频信息失败");
        return -1;
    }

    int i=0;
    for(int i=0;i<formatContext->nb_streams;i++)
    {
        if(formatContext->streams[i]->codec->codec_type==AVMEDIA_TYPE_AUDIO)
        {
            LOGE("找到音频id %d", formatContext->streams[i]->codec->codec_type);
            audio_stream_index=1;
            break;
        }
    }

    codecContext=formatContext->streams[audio_stream_index]->codec;
    LOGE("获取视频编码上下文 %p", codecContext);

    codex=avcodec_find_decoder(codecContext->codec_id);
    LOGE("获取视频编码 %p", codex);

    if(avcodec_open2(codecContext, codex, NULL)<0)
    {

    }
    packet=(AVPacket *) av_malloc(sizeof(AVPacket));

    frame=av_frame_alloc();
    swrContext=swr_alloc();

    int length=0;
    int got_frame;

    out_buffer=(uint8_t *) av_malloc(44100*2);
    uint64_t out_ch_layout=AV_CH_LAYOUT_STEREO;

    enum AVSampleFormat out_format=AV_SAMPLE_FMT_S16;

    int out_sample_rate=codecContext->sample_rate;

    swr_alloc_set_opts(swrContext, out_ch_layout, out_format, out_sample_rate,
            codecContext->channel_layout, codecContext->sample_fmt, codecContext->sample_rate, 0, NULL);

    swr_init(swrContext);
    out_channel_nb=av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
    *rate=codecContext->sample_rate;
    *channel=codecContext->channels;
    return 0;
}

int getPcm(void **pcm, size_t *pcm_size)
{
    int frameCount=0;
    int got_frame;

    while(av_read_frame(formatContext, packet)>=0)
    {
        avcodec_decode_audio4(codecContext, frame, &got_frame, packet);
        if(got_frame)
        {
            LOGE("解码");

            swr_convert(swrContext, &out_buffer, 44100*2, (const uint8_t **)frame->data, frame->nb_samples);
            int size=av_samples_get_buffer_size(NULL, out_channer_nb, frame->nb_samples, AV_SAMPLE_FMT_S16, 1);

            *pcm=out_buffer;
            *pcm_size=size;
            break;
        }
    }
    return 0;
}

void realseFFmpeg()
{
    av_free_packet(packet);
    av_free(out_buffer);
    av_frame_free(&frame);
    swr_free(&swrContext);
    avcodec_close(codecContext);
    avformat_close_input(&formatContext);
}
