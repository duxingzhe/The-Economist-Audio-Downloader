LOCAL_PATH := $(call my-dir)

LOCAL_LIB_FILE_PATH_PREFIX := $(LOCAL_PATH)/../../../../../commonlib/src/main/jni/ffmpeg

include $(CLEAR_VARS)
LOCAL_MODULE := libavcodec
LOCAL_SRC_FILES := $(LOCAL_LIB_FILE_PATH_PREFIX)/$(TARGET_ARCH_ABI)/$(LOCAL_MODULE).so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_LIB_FILE_PATH_PREFIX)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libavdevice
LOCAL_SRC_FILES := $(LOCAL_LIB_FILE_PATH_PREFIX)/$(TARGET_ARCH_ABI)/$(LOCAL_MODULE).so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_LIB_FILE_PATH_PREFIX)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libavfilter
LOCAL_SRC_FILES := $(LOCAL_LIB_FILE_PATH_PREFIX)/$(TARGET_ARCH_ABI)/$(LOCAL_MODULE).so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_LIB_FILE_PATH_PREFIX)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libavformat
LOCAL_SRC_FILES := $(LOCAL_LIB_FILE_PATH_PREFIX)/$(TARGET_ARCH_ABI)/$(LOCAL_MODULE).so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_LIB_FILE_PATH_PREFIX)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libavutil
LOCAL_SRC_FILES := $(LOCAL_LIB_FILE_PATH_PREFIX)/$(TARGET_ARCH_ABI)/$(LOCAL_MODULE).so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_LIB_FILE_PATH_PREFIX)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libpostproc
LOCAL_SRC_FILES := $(LOCAL_LIB_FILE_PATH_PREFIX)/$(TARGET_ARCH_ABI)/$(LOCAL_MODULE).so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_LIB_FILE_PATH_PREFIX)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libswresample
LOCAL_SRC_FILES := $(LOCAL_LIB_FILE_PATH_PREFIX)/$(TARGET_ARCH_ABI)/$(LOCAL_MODULE).so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_LIB_FILE_PATH_PREFIX)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libswscale
LOCAL_SRC_FILES := $(LOCAL_LIB_FILE_PATH_PREFIX)/$(TARGET_ARCH_ABI)/$(LOCAL_MODULE).so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_LIB_FILE_PATH_PREFIX)/include
include $(PREBUILT_SHARED_LIBRARY)

LOCAL_PATH:= $(call my-dir)
