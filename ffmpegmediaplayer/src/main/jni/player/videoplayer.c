#include <videoplayer.h>

const int TARGET_IMAGE_FORMAT=AV_PIX_FMT_RGBA;
const int TARGET_IMAGE_CODEC=AV_CODEC_ID_PNG;

void createVideoEngine(VideoPlayer **ps)
{
    VideoPlayer *is=*ps;
}

void createScreen(VideoPlayer **ps, void *surface, int width, int height)
{
    VideoPlayer *is=*ps;
    is->native_window=surface;
}

struct SwsContext *createScaler(VideoPlayer **ps, AVCodecContext *codec)
{
    struct SwsContext *sws_ctx;

    sws_ctx=sws_getContext(codec->width, codec->height, codec->pix_fmt,
            codec->width, codec->height, AV_PIX_FMT_RGBA, SWS_BILINEAR, NULL, NULL, NULL);

    return sws_ctx;
}

void *createBmp(VideoPlayer **ps, int width, int height)
{
    VideoPlayer *is= *ps;

    return malloc(sizeof(Picture));
}