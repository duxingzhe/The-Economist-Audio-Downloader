LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := ffmpeg_mediametadataretriever_jni
LOCAL_CFLAGS :=
LOCAL_SRC_FILES := luxuan_media_MediaMetadataRetriever.cpp \
	mediametadataretriever.cpp \
        ffmpeg_mediametadataretriever.c \
        ffmpeg_utils.c
LOCAL_SHARED_LIBRARIES := libswresample-3 libswscale-5 libavcodec-58 libavformat-58 libavutil-56 libavfilter-7 libssl libcrypto
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../ffmpeg/include
LOCAL_LDLIBS := -llog
LOCAL_LDLIBS += -landroid
LOCAL_LDLIBS += -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
