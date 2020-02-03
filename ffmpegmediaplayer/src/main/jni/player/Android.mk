LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LIB_FFMPEG_FILE_PATH_PREFIX := $(LOCAL_PATH)/../../../../../commonlib/src/main/jni/ffmpeg

LOCAL_MODULE := ffmpeg_mediaplayer_jni
SDL_PATH := ../SDL
LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(SDL_PATH)/include $(LOCAL_PATH)/$(SDL_PATH)/src/video/android
LOCAL_CFLAGS := 
LOCAL_SRC_FILES := luxuan_media_MediaPlayer.cpp \
		        mediaplayer.cpp \
		        ffmpeg_mediaplayer.c \
               audioplayer.c \
               videoplayer.c \
               ffmpeg_utils.c
LOCAL_SHARED_LIBRARIES := SDL2 libswresample libswscale libavcodec libavformat libavutil libavfilter libssl libcrypto
LOCAL_C_INCLUDES := $(LOCAL_LIB_FFMPEG_FILE_PATH_PREFIX)/include
# for native audio
LOCAL_LDLIBS += -lOpenSLES -lGLESv1_CM -lGLESv2
# for logging
LOCAL_LDLIBS += -llog
LOCAL_LDLIBS += -landroid
LOCAL_LDLIBS += -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
