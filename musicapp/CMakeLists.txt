cmake_minimum_required(VERSION 3.4.1)

set(lib_DIR F:\\Project\\Android\\NDK\\musiclib\\libs)

add_library( native-lib
        SHARED
        src/main/cpp/native-lib.cpp
        )
add_library( ssl
        SHARED
        IMPORTED)
set_target_properties( ssl
        PROPERTIES IMPORTED_LOCATION
        ${lib_DIR}/${ANDROID_ABI}/libssl.so)
add_library( crypto
        SHARED
        IMPORTED)
set_target_properties( crypto
        PROPERTIES IMPORTED_LOCATION
        ${lib_DIR}/${ANDROID_ABI}/libcrypto.so)
add_library( avcodec-58
             SHARED
             IMPORTED)
set_target_properties( avcodec-58
                       PROPERTIES IMPORTED_LOCATION
                       ${lib_DIR}/${ANDROID_ABI}/libavcodec-58.so)
add_library( avfilter-7
             SHARED
             IMPORTED)
set_target_properties( avfilter-7
                       PROPERTIES IMPORTED_LOCATION
                       ${lib_DIR}/${ANDROID_ABI}/libavfilter-7.so)

add_library( avformat-58
             SHARED
             IMPORTED)
set_target_properties( avformat-58
                       PROPERTIES IMPORTED_LOCATION
                       ${lib_DIR}/${ANDROID_ABI}/libavformat-58.so)

add_library( avutil-56
             SHARED
             IMPORTED)
set_target_properties( avutil-56
                       PROPERTIES IMPORTED_LOCATION
                       ${lib_DIR}/${ANDROID_ABI}/libavutil-56.so)

add_library( swresample-3
             SHARED
             IMPORTED)
set_target_properties( swresample-3
                       PROPERTIES IMPORTED_LOCATION
                       ${lib_DIR}/${ANDROID_ABI}/libswresample-3.so)

add_library( swscale-5
             SHARED
             IMPORTED)
set_target_properties( swscale-5
                       PROPERTIES IMPORTED_LOCATION
                       ${lib_DIR}/${ANDROID_ABI}/libswscale-5.so)
include_directories(${lib_DIR}/../src/main/cpp
        ${lib_DIR}/include/ffmpeg
        ${lib_DIR}/include/openssl
        src/main/cpp
        )
target_link_libraries( native-lib
                       avcodec-58
                       avfilter-7
                       avformat-58
                       avutil-56
                       swresample-3
                       swscale-5
                       log
                       android
                       OpenSLES
                       )