cmake_minimum_required(VERSION 3.4.1)

set(CURRENT_DIR ${CMAKE_SOURCE_DIR})
set(distribution_DIR ${CURRENT_DIR}/../commonlib/src/main/jni)

add_library( native-lib
        SHARED
        src/main/cpp/native-lib.cpp
        src/main/cpp/FFmpegMusic.cpp
        )
add_library( ssl
        SHARED
        IMPORTED)
set_target_properties( ssl
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/openssl/${ANDROID_ABI}/libssl.so)
add_library( crypto
        SHARED
        IMPORTED)
set_target_properties( crypto
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/openssl/${ANDROID_ABI}/libcrypto.so)
add_library( avcodec
        SHARED
        IMPORTED)
set_target_properties( avcodec
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/ffmpeg/${ANDROID_ABI}/libavcodec.so)
add_library( avfilter
        SHARED
        IMPORTED)
set_target_properties( avfilter
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/ffmpeg/${ANDROID_ABI}/libavfilter.so)

add_library( avformat
        SHARED
        IMPORTED)
set_target_properties( avformat
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/ffmpeg/${ANDROID_ABI}/libavformat.so)

add_library( avutil
        SHARED
        IMPORTED)
set_target_properties( avutil
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/ffmpeg/${ANDROID_ABI}/libavutil.so)

add_library( swresample
        SHARED
        IMPORTED)
set_target_properties( swresample
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/ffmpeg/${ANDROID_ABI}/libswresample.so)

add_library( swscale
        SHARED
        IMPORTED)
set_target_properties( swscale
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/ffmpeg/${ANDROID_ABI}/libswscale.so)
include_directories(../musiclib/src/main/cpp
        ${distribution_DIR}/ffmpeg/include
        ${distribution_DIR}/openssl/include
        src/main/cpp
        )
target_link_libraries( native-lib
                        avcodec
                        avfilter
                        avformat
                        avutil
                        swresample
                        swscale
                        ssl
                        crypto
                        log
                        android
                        OpenSLES
                        )
