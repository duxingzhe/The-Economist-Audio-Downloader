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
add_library( avcodec-58
        SHARED
        IMPORTED)
set_target_properties( avcodec-58
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/ffmpeg/${ANDROID_ABI}/libavcodec.so)
add_library( avfilter-7
        SHARED
        IMPORTED)
set_target_properties( avfilter-7
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/ffmpeg/${ANDROID_ABI}/libavfilter.so)

add_library( avformat-58
        SHARED
        IMPORTED)
set_target_properties( avformat-58
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/ffmpeg/${ANDROID_ABI}/libavformat.so)

add_library( avutil-56
        SHARED
        IMPORTED)
set_target_properties( avutil-56
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/ffmpeg/${ANDROID_ABI}/libavutil.so)

add_library( swresample-3
        SHARED
        IMPORTED)
set_target_properties( swresample-3
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/ffmpeg/${ANDROID_ABI}/libswresample.so)

add_library( swscale-5
        SHARED
        IMPORTED)
set_target_properties( swscale-5
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
