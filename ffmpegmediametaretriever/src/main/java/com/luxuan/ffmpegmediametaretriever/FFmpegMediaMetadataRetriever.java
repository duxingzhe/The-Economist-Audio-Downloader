package com.luxuan.ffmpegmediametaretriever;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
            "crypto",
            "ffmpeg_mediametadataretriever_jni"
    };

    static{
        for(int i=0;i<JNI_LIBRARIES.length;i++) {
            System.loadLibrary(JNI_LIBRARIES[i]);
        }

        native_init();
    }

    private long mNativeContext;

    public FFmpegMediaMetadataRetriever(){
        native_setup();
    }

    public native void setDataSource(String path)throws IllegalStateException;

    public void setDataSource(String uri, Map<String, String> headers) throws IllegalArgumentException{
        int i=0;
        String[] keys=new String[headers.size()];
        String[] values=new String[headers.size()];
        for(Map.Entry<String, String> entry: headers.entrySet()) {
            keys[i]=entry.getKey();
            values[i]=entry.getValue();
            i++;
        }

        _setDataSource(uri, keys, values);
    }

    private native void _setDataSource(String uri, String[] keys, String[] values) throws IllegalArgumentException;

    public native void setDataSource(FileDescriptor fd, long offset, long length) throws IllegalArgumentException;

    public void setDataSource(FileDescriptor fd) throws IllegalArgumentException{
        setDataSource(fd, 0, 0x7ffffffffffffffL);
    }

    public void setDataSource(Context context, Uri uri) throws IllegalArgumentException, SecurityException {
        if(uri==null){
            throw new IllegalArgumentException();
        }

        String scheme=uri.getScheme();
        if(scheme==null||scheme.equals("file")){
            setDataSource(uri.getPath());
            return;
        }

        AssetFileDescriptor fd=null;
        try{
            ContentResolver resolver=context.getContentResolver();
            try{
                fd=resolver.openAssetFileDescriptor(uri, "r");
            }catch(FileNotFoundException e) {
                throw new IllegalArgumentException();
            }

            FileDescriptor descriptor=fd.getFileDescriptor();
            if(!descriptor.valid()){
                throw new IllegalArgumentException();
            }

            if(fd.getDeclaredLength()<0){
                setDataSource(descriptor);
            }else{
                setDataSource(descriptor, fd.getStartOffset(), fd.getDeclaredLength());
            }
            return;
        }catch(SecurityException ex){

        }finally{
            try{
                if(fd!=null) {
                    fd.close();
                }
            }catch(IOException ioException){

            }
        }

        setDataSource(uri.toString());
    }

    public native String extractMetadata(String key);

    public native String extractMetadataFromChapter(String key, int chapter);

    public Metadata getMetadata(){
        boolean update_only=false;
        boolean apply_filter=false;

        Metadata data=new Metadata();
        HashMap<String, String> metadata=null;
        if((metadata=native_getMetadata(update_only, apply_filter, metadata))==null){
            return null;
        }

        if(!data.parse(metadata)){
            return null;
        }

        return data;
    }

    private native final HashMap<String, String> native_getMetadata(boolean update_only, boolean apply_filter, HashMap<String, String> reply);

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
