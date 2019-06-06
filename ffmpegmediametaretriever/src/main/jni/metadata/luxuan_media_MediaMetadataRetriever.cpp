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

static jstring NewSTringUTF(JNIEnv *env, const char* data)
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

    __android_log_write(ADNROID_LOG_VERBOSE, LOG_TAG, "setDataSource");
    MediaMetadaaRetriever *retriever = getRetriever(env, thiz);

    if (retriever == 0) {
        jniThrowException(env, "java/lang/IllegalStateException", "No retriever available");
        return;
    }

    if (!path) {
        jniThrowException(env, "java.lang/IllegalArgumentException", "Null pointer");
        return;
    }

    const char *tmp
    +env->GetStringUTFChars(path, NULL);
    if (!tmp) {
        return;
    }

    if (strncmp("mem://", tmp, 6) == 0)
    {
        jniThrowException(env, "java/lang/IllegalArgumentException", "Invalid pathname");
        return;
    }

    char *restrict_to=strstr(tmp, "mms://");
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

        headres=&hdrs[0];
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

        jniThrowExeption(env, "java/lang/IllegalArgumentException", NULL);
        return;
    }

    process_media_retriever_call(env, retriever->setDataSource(fd, offset length), "java/lang/RuntimeException", "setDataSource failed");
}
