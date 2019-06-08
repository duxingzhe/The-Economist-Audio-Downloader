//
// Created by Administrator on 2019/6/1.
//

#define LOG_TAG "MediaMetadataRetrieverJNI"

#include <assert.h>
#include <android/log.h>
#include <mediametadataretriever.h>
#include "jni.h"

#include <android/bitmap.h>
#include <android/native_window_jni.h>

extern "C"
{
#include "ffmpeg_mediametadataretriever.h"
}

using namespace std;

struct fields_t
{
    jfieldID context;
};

static fields_t fields;
static const char* const kClassPathName="luxuan/media/FFmpegMediaMetadataRetriever";

static JavaVM *m_vm;

static ANativeWindow* theNativeWindow;

static jstring NewStringUTF(JNIEnv *env, const char* data)
{
    jstring str=NULL;

    int size=strlen(data);

    jbyteArray array=NULL;
    array=env->NewByteArray(size);
    if(!array)
    {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "convertString: OutOfMemoeryError is thrown.");
    }
    else
    {
        jbyte* bytes=env->GetByteArrayElements(array, NULL);
        if(bytes!=NULL)
        {
            memcpy(bytes, data, size);
            env->ReleaseByteArrayElements(array, bytes, 0);

            jclass string_Clazz=env->FindClass("java/lang/String");
            jmethodID string_initMethodID=env->GetMethodID(string_Clazz, "<init>", "([BLjava/lang/String;)V");
            jstring utf=env->NewStringUTF("UTF-8");
            str=(jstring)env->NewObject(string_Clazz, string_initMethodID, array, utf);

            env->DeleteLocalRef(utf);
        }
    }

    env->DeleteLocalRef(array);

    return str;
}

void jniThrowException(JNIEnv *env, const char* className, const char* msg)
{
    jclass exception=env->FindClass(className);
    env->ThrowNew(exception, msg);
}

static void process_media_retriever_call(JNIEnv *env, int opStatus, const char* exception, const char *message)
{
    if(opStatus==-2)
    {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
    }
    else if(opStatus==-1)
    {
        if(strlen(message)>230)
        {
            jniThrowException(env, exception, message);
        }
        else
        {
            char msg[256];
            sprintf(msg, "%s: status=0x%X", message, opStatus);
            jniThrowException(env, exception, msg);
        }
    }
}

static MediaMetadataRetriever* getRetriever(JNIEnv *env, jobject thiz)
{
    MediaMetadataRetriever* retriever=(MediaMetadataRetriever*) env->GetLongField(thiz, fields.context);
    return retriever;
}

static void setRetriever(JNIEnv *env, jobject thiz, long retriever)
{
    MediaMetadataRetriever *old=(MediaMetadataRetriever*) env->GetLongField(thiz, fields.context);
    env->SetLongField(thiz, fields.context, retriever);
}

static void luxuan_media_FFmpegMediaMetadataRetriever_setDataSourceAndHeaders(JNIEnv *env, jobject thiz, jstring path,
        jobjectArray keys, jobjectArray values) {

    __android_log_write(ANDROID_LOG_VERBOSE, LOG_TAG, "setDataSource");
    MediaMetadataRetriever *retriever = getRetriever(env, thiz);

    if (retriever == 0) {
        jniThrowException(env, "java/lang/IllegalStateException", "No retriever available");
        return;
    }

    if (!path) {
        jniThrowException(env, "java.lang/IllegalArgumentException", "Null pointer");
        return;
    }

    const char *tmp;
    +env->GetStringUTFChars(path, NULL);
    if (!tmp) {
        return;
    }

    if (strncmp("mem://", tmp, 6) == 0)
    {
        jniThrowException(env, "java/lang/IllegalArgumentException", "Invalid pathname");
        return;
    }

    char *restrict_to=(char *)strstr(tmp, "mms://");
    if(restrict_to)
    {
        strncpy(restrict_to, "mmsh://", 6);
        puts(tmp);
    }

    char *headers=NULL;

    if(keys&&values!=NULL)
    {
        int keysCount=env->GetArrayLength(keys);
        int valuesCount=env->GetArrayLength(values);

        if(keysCount!=valuesCount)
        {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "keys and values arrays have different length");
            jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
            return;
        }

        int i=0;
        const char *rawString=NULL;
        char hdrs[2048];

        for(i=0;i<keysCount;i++)
        {
            jstring key=(jstring) env->GetObjectArrayElement(keys, i);
            rawString=env->GetStringUTFChars(key, NULL);
            strcat(hdrs, rawString);
            strcat(hdrs, ": ");
            env->ReleaseStringUTFChars(key, rawString);

            jstring value=(jstring) env->GetObjectArrayElement(values, i);
            rawString=env->GetStringUTFChars(value, NULL);
            strcat(hdrs, rawString);
            strcat(hdrs, "\r\n");
            env->ReleaseStringUTFChars(value, rawString);
        }

        headers=&hdrs[0];
    }

    process_media_retriever_call(env, retriever->setDataSource(tmp, headers), "java/lang/IllegalArgumentException",
            "setDataSource failed");

    env->ReleaseStringUTFChars(path, tmp);
    tmp=NULL;
}

static void luxuan_media_FFmpegMediaMetadataRetriever_setDataSource(JNIEnv *env, jobject thiz, jstring path)
{
    luxuan_media_FFmpegMediaMetadataRetriever_setDataSourceAndHeaders(env, thiz, path, NULL, NULL);
}

static int jniGetFDFromFileDescriptor(JNIEnv *env, jobject fileDescriptor)
{
    jint fd=-1;
    jclass fdClass=env->FindClass("java/io/FileDescriptor");

    if(fdClass!=NULL)
    {
        jfieldID fdClassDescriptorFieldID=env->GetFieldID(fdClass, "descriptor", "I");
        if(fdClassDescriptorFieldID!=NULL&&fileDescriptor!=NULL)
        {
            fd=env->GetIntField(fileDescriptor, fdClassDescriptorFieldID);
        }
    }

    return fd;
}

static void luxuan_media_FFmpegMediaMetadataRetriever_setDataSourceFD(JNIEnv *env, jobject thiz, jobject fileDescriptor, jlong offset, jlong length)
{
    __android_log_write(ANDROID_LOG_VERBOSE, LOG_TAG, "setDataSource");
    MediaMetadataRetriever *retriever=getRetriever(env, thiz);
    if(retriever==0)
    {
        jniThrowException(env, "java/lang/IllegalStateException", "No retriever available");
        return;
    }
    if(!fileDescriptor)
    {
        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        return;
    }

    int fd=jniGetFDFromFileDescriptor(env, fileDescriptor);
    if(offset<0||length<0||fd<0)
    {
        if(offset<0)
        {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "negative offset (%lld)", offset);
        }
        if(length<0)
        {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "negative length (%lld)", length);
        }
        if(fd<0)
        {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "invalid file descriptor");
        }

        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        return;
    }

    process_media_retriever_call(env, retriever->setDataSource(fd, offset, length), "java/lang/RuntimeException", "setDataSource failed");
}

static jbyteArray luxuan_media_FFmpegMediaMetadataRetriever_getFrameAtTime(JNIEnv *env, jobject thiz, jlong timeUs, jint option)
{
    MediaMetadataRetriever* retriever=getRetriever(env, thiz);

    if(retriever==0)
    {
        jniThrowException(env, "java/lang/IllegalStateException ", "No retriever available");
        return NULL;
    }

    AVPacket packet;
    av_init_packet(&packet);
    jbyteArray array=NULL;

    if(retriever->getFrameAtTime(timeUs, option, &packet)==0)
    {
        int size=packet.size;
        uint8_t *data=packet.data;
        array=env->NewByteArray(size);
        if(!array)
        {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "getFrameAtTime: OutOfMemoryError is thrown.");
        }
        else
        {
            jbyte* bytes=env->GetByteArrayElements(array, NULL);
            if(bytes!=NULL)
            {
                memcpy(bytes,data,size);
                env->ReleaseByteArrayElements(array, bytes, 0);
            }
        }
    }

    av_packet_unref(&packet);

    return array;
}

static jbyteArray luxuan_media_FFmpegMediaMetadataRetriever_getScaledFrameAtTime(JNIEnv*env, jobject thiz, jlong timeUs, jint option, jint width, jint height) {
    MediaMetadataRetriever *retriever = getRetriever(env, thiz);

    if (retriever == 0)
    {
        jniThrowException(env, "java/lang/IllegalStateExcetion ", "No retriever available");
        return NULL;
    }

    AVPacket packet;
    av_init_packet(&packet);
    jbyteArray array = NULL;

    if (retriever->getScaledFrameAtTime(timeUs, option, &packet, width, height) == 0) {
        int size = packet.size;
        uint8_t *data = packet.data;
        array = env->NewByteArray(size);
        if (!array) {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
                                "getFrameAtTime: OutOfMemoryError is thrown.");
        } else {
            jbyte *bytes = env->GetByteArrayElements(array, NULL);
            if (bytes != NULL) {
                memcpy(bytes, data, size);
                env->ReleaseByteArrayElements(array, bytes, 0);
            }
        }
    }

    av_packet_unref(&packet);

    return array;
}

static jbyteArray luxuan_media_FFmpegMediaMetadataRetriever_getEmbeddedPicture(JNIEnv *env, jobject thiz)
{
    MediaMetadataRetriever *retriever = getRetriever(env, thiz);
    if (retriever == 0) {
        jniThrowException(env, "java/lang/IllegalStateException", "No retriever available");
        return NULL;
    }

    AVPacket packet;
    av_init_packet(&packet);
    jbyteArray array = NULL;

    if (retriever->extractAlbumArt(&packet) == 0)
    {
        int size = packet.size;
        uint8_t *data = packet.data;
        array = env->NewByteArray(size);
        if (!array)
        {

        }
        else
        {
            jbyte *bytes = env->GetByteArrayElements(array, NULL);
            if (bytes != NULL)
            {
                memcpy(bytes, data, size);
                env->ReleaseByteArrayElements(array, bytes, 0);
            }
        }
    }

    av_packet_unref(&packet);

    return array;
}

static jobject luxuan_media_FFmpegMediaMetadataRetriever_extractMetadata(JNIEnv *env, jobject thiz, jstring jkey)
{
    MediaMetadataRetriever* retriever = getRetriever(env, thiz);
    if (retriever == 0)
    {
        jniThrowException(env, "java/lang/IllegalStateException", "No retriever available");
        return NULL;
    }

    if(!jkey)
    {
        jniThrowException(env, "java/lang/IllegalArgumentException", "Null pointer)");
        return NULL;
    }

    const char *key=env->GetStringUTFChars(jkey, NULL);
    if(!key)
    {
        return NULL;
    }

    const char* value=retriever->extractMetadata(key);
    if(!value)
    {
        return NULL;
    }

    env->ReleaseStringUTFChars(jkey, key);
    return NewStringUTF(env, value);
}

static jobject luxuan_media_FFmpegMediaMetadataRetriever_extractMetadataFromChapter(JNIEnv *env, jobject thiz, jstring jkey, jint chapter)
{
    MediaMetadataRetriever* retriever = getRetriever(env, thiz);
    if (retriever == 0)
    {
        jniThrowException(env, "java/lang/IllegalStateException", "No retriever available");
        return NULL;
    }

    const char *key=env->GetStringUTFChars(jkey, NULL);
    if(!key)
    {
        return NULL;
    }

    if(chapter<0)
    {
        return NULL;
    }

    const char* value=retriever->extractMetadataFromChapter(key, chapter);
    if(!value)
    {
        return NULL;
    }

    env->ReleaseStringUTFChars(jkey, key);
    return env->NewStringUTF(value);
}

static jobject luxuan_media_FFmpegMediaMetadataRetriever_getMetadata(JNIEnv *env, jobject thiz, jboolean update_only, jboolean apply_filter, jobject reply)
{
    MediaMetadataRetriever* retriever = getRetriever(env, thiz);
    if (retriever == 0) {
        jniThrowException(env, "java/lang/IllegalStateException", "No retriever available");
        return JNI_FALSE;
    }

    AVDictionary *metadata=NULL;

    if(retriever->getMetadata(update_only, apply_filter, &metadata)==0)
    {
        jclass hashMap_Clazz=env->FindClass("java/util/HashMap");
        jmethodID gHashMap_initMethodID=env->GetMethodID(hashMap_Clazz, "<init>", "()V");
        jobject map=env->NewObject(hashMap_Clazz, gHashMap_initMethodID);
        jmethodID gHashMap_putMethodID=env->GetMethodID(hashMap_Clazz, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

        int i=0;

        for(i=0;i<metadata->count;i++)
        {
            jstring jKey=NewStringUTF(env, metadata->elems[i].key);
            jstring jValue=NewStringUTF(env, metadata->elems[i].value);
            (jobject)env->CallObjectMethod(map, gHashMap_putMethodID, jKey, jValue);
            env->DeleteLocalRef(jKey);
            env->DeleteLocalRef(jValue);
        }

        if(metadata)
        {
            av_dict_free(&metadata);
        }

        return map;
    }
    else
    {
        return reply;
    }
}

static void luxuan_media_FFmpegMediaMetadataRetriever_release(JNIEnv *env, jobject thiz)
{
    __android_log_write(ANDROID_LOG_INFO, LOG_TAG, "release");
    MediaMetadataRetriever* retriever=getRetriever(env, thiz);
    delete retriever;
    setRetriever(env, thiz, 0);
}

static void luxuan_media_FFmpegMediaMetadataRetriever_setSurface(JNIEnv *env, jclass thiz, jobject surface)
{
    MediaMetadataRetriever* retriever=getRetriever(env, thiz);
    if (retriever == 0) {
        jniThrowException(env, "java/lang/IllegalStateException", "No retriever available");
        return;
    }

    theNativeWindow=ANativeWindow_fromSurface(env, surface);

    if(theNativeWindow!=NULL)
    {
        retriever->setNativeWindow(theNativeWindow);
    }
}

static void luxuan_media_FFmpegMediaMetadataRetriever_native_finalize(JNIEnv *env, jobject thiz)
{
    luxuan_media_FFmpegMediaMetadataRetriever_release(env, thiz);
}

static void luxuan_media_FFmpegMediaMetadataRetriever_native_init(JNIEnv *env, jobject thiz)
{
    __android_log_write(ANDROID_LOG_INFO, LOG_TAG, "naitve_init");
    jclass clazz=env->FindClass(kClassPathName);
    if(clazz==NULL)
    {
        return;
    }

    fields.context=env->GetFieldID(clazz, "mNativeContext", "J");
    if(fields.context==NULL)
    {
        return;
    }

    av_register_all();
    avformat_network_init();
}

static void luxuan_media_FFmpegMediaMetadataRetriever_native_setup(JNIEnv *env, jobject thiz)
{
    __android_log_write(ANDROID_LOG_INFO, LOG_TAG, "native_setup");
    MediaMetadataRetriever* retriever=new MediaMetadataRetriever();
    if(retriever==0)
    {
        jniThrowException(env, "java/lang/RuntimeException", "Out of memory");
        return;
    }
    setRetriever(env, thiz, (long)retriever);
}

// JNI mapping between Java methods and native methods
static JNINativeMethod nativeMethods[] = {
        {"setDataSource", "(Ljava/lang/String;)V", (void *)luxuan_media_FFmpegMediaMetadataRetriever_setDataSource},

        {
         "_setDataSource",
                          "(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)V",
                                                   (void *)luxuan_media_FFmpegMediaMetadataRetriever_setDataSourceAndHeaders
        },

        {"setDataSource", "(Ljava/io/FileDescriptor;JJ)V", (void *)luxuan_media_FFmpegMediaMetadataRetriever_setDataSourceFD},
        {"_getFrameAtTime", "(JI)[B", (void *)luxuan_media_FFmpegMediaMetadataRetriever_getFrameAtTime},
        {"_getScaledFrameAtTime", "(JIII)[B", (void *)luxuan_media_FFmpegMediaMetadataRetriever_getScaledFrameAtTime},
        {"extractMetadata", "(Ljava/lang/String;)Ljava/lang/String;", (void *)luxuan_media_FFmpegMediaMetadataRetriever_extractMetadata},
        {"extractMetadataFromChapter", "(Ljava/lang/String;I)Ljava/lang/String;", (void *)luxuan_media_FFmpegMediaMetadataRetriever_extractMetadataFromChapter},
        {"native_getMetadata", "(ZZLjava/util/HashMap;)Ljava/util/HashMap;", (void *)luxuan_media_FFmpegMediaMetadataRetriever_getMetadata},
        {"getEmbeddedPicture", "()[B", (void *)luxuan_media_FFmpegMediaMetadataRetriever_getEmbeddedPicture},
        {"release", "()V", (void *)luxuan_media_FFmpegMediaMetadataRetriever_release},
        {"setSurface", "(Ljava/lang/Object;)V", (void *)luxuan_media_FFmpegMediaMetadataRetriever_setSurface},
        {"native_finalize", "()V", (void *)luxuan_media_FFmpegMediaMetadataRetriever_native_finalize},
        {"native_setup", "()V", (void *)luxuan_media_FFmpegMediaMetadataRetriever_native_setup},
        {"native_init", "()V", (void *)luxuan_media_FFmpegMediaMetadataRetriever_native_init},
};
