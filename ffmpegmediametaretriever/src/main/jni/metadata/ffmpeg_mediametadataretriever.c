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
