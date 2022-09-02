LOCAL_PATH := $(call my-dir)

###########################
#
# SDL shared library
#
###########################

include $(CLEAR_VARS)
LOCAL_MODULE := libSDL2
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/$(LOCAL_MODULE).so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)
