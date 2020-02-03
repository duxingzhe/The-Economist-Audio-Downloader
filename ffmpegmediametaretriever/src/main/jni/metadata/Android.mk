LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LIB_FFMPEG_FILE_PATH_PREFIX := $(LOCAL_PATH)/../../../../../commonlib/src/main/jni/ffmpeg

LOCAL_MODULE := ffmpeg_mediametadataretriever_jni
LOCAL_CFLAGS :=
LOCAL_SRC_FILES := luxuan_media_MediaMetadataRetriever.cpp \
	mediametadataretriever.cpp \
        ffmpeg_mediametadataretriever.c \
        ffmpeg_utils.c
LOCAL_SHARED_LIBRARIES := libswresample libswscale libavcodec libavformat libavutil libavfilter libssl libcrypto
LOCAL_C_INCLUDES := $(LOCAL_LIB_FFMPEG_FILE_PATH_PREFIX)/include
LOCAL_LDLIBS := -llog
LOCAL_LDLIBS += -landroid
LOCAL_LDLIBS += -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
