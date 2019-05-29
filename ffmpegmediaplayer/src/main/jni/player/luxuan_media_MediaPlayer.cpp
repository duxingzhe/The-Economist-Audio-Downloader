//
// Created by Administrator on 2019/5/16.
//
#define LOG_TAG "FFmpegMediaPlayer-JNI"

#include "android/log.h"
#include <mediaplayer.h>
#include <stdio.h>
#include <assert.h>
#include <string.h>
#include "jni.h"
#include "Errors.h"

#include <android/bitmap.h>
#include <android/native_window_jni.h>

extern "C"
{
#include "ffmpeg_mediaplayer.h"
}

using namespace std;

struct fields_t
{
    jfieldID context;
    jfieldID surface_texture;

    jmethodID post_event;
};

static fields_t fields;

static JavaVM *m_vm;

class JNIMediaPlayerListener: public MediaPlayerListener
{
public:
    JNIMediaPlayerListener(JNIEnv *env, jobject thiz, jobject weak_thiz);
    ~JNIMediaPlayerListener();
    virtual void notify(int msg, int ext1, int ext2, int from_thread);

private:
    JNIMediaPlayerListener();
    jclass mClass;
    jobject mObject;
    jobject mThiz;
};

void jniThrowException(JNIEnv *env, const char *className, const char *msg)
{
    jclass exception = env->FindClass(className);
    env->ThrowNew(exception, msg);
}

JNIMediaPlayerListener::JNIMediaPlayerListener(JNIEnv* env, jobject thiz, jobject weak_thiz)
{
    jclass clazz=env->GetObjectClass(thiz);
    if(clazz==NULL)
    {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Can't find luxuan/media/FFmpegMediaPlayer");
        jniThrowException(env, "java/lang/Exception", NULL);
        return;
    }
    mClass=(jclass)env->NewGlobalRef(clazz);
    mThiz=(jobject)env->NewGlobalRef(thiz);
    mObject=env->NewGlobalRef(weak_thiz);
}

void JNIMediaPlayerListener::notify(int msg, int ext1, int ext2, int fromThread)
{
    __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "notify: %d", msg);
    JNIEnv *env=0;
    int isAttached=0;

    int status=m_vm->GetEnv((void **)&env, JNI_VERSION_1_6);

    if(fromThread)
    {
        jclass *interface_class;

        isAttached=0;

        if(m_vm->AttachCurrentThread(&env, NULL)<0)
        {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "failed to attach current thread");
        }

        isAttached=1;
    }

    env->CallStaticVoidMethod(mClass, fields.post_event, mObject, msg, ext1, ext2, NULL);

    if(env->ExceptionCheck())
    {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "An exception occurred while notifying an event.");
        env->ExceptionClear();
    }

    if(fromThread&&isAttached)
    {
        m_vm->DetachCurrentThread();
    }
}
