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
