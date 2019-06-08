package com.luxuan.ffmpegmediametaretriever;

import android.graphics.Bitmap;

public class FFmpegMediaMetadataRetriever {

    private final static String TAG="FFmpegMediaMetadataRetriever";

    public static Bitmap.Config IN_PREFERRED_CONFIG;

    private static final String[] JNI_LIBRARIES={
            "avutil-56",
            "swscale-5",
            "swresample-3",
            "avcodec-58",
            "avformart-58",
            "postproc-55",
            "ssl",
            "ffmpeg_mediametadataretriever_jni"
    };

    static{
        for(int i=0;i<JNI_LIBRARIES.length;i++) {
            System.loadLibrary(JNI_LIBRARIES[i]);
        }

        native_init();
    }

    private static native void native_init();

    /**
     * Check a parcel containing metadata is well formed. The header
     * is checked as well as the individual records format. However, the
     * data inside the record is not checked because we do lazy access
     * (we check/unmarshall only data the user asks for.)
     *
     * Format of a metadata parcel:
     <pre>
     1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                     metadata total size                       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |     'M'       |     'E'       |     'T'       |     'A'       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                                                               |
     |                .... metadata records ....                     |
     |                                                               |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     </pre>
     *
     * {@hide}
     */
}
