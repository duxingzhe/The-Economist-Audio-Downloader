package com.luxuan.ffmpegmediametaretriever;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
        if (uri == null) {
            throw new IllegalArgumentException();
        }

        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals("file")) {
            setDataSource(uri.getPath());
            return;
        }

        AssetFileDescriptor fd = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            try {
                fd = resolver.openAssetFileDescriptor(uri, "r");
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException();
            }

            FileDescriptor descriptor = fd.getFileDescriptor();
            if (!descriptor.valid()) {
                throw new IllegalArgumentException();
            }

            if (fd.getDeclaredLength() < 0) {
                setDataSource(descriptor);
            } else {
                setDataSource(descriptor, fd.getStartOffset(), fd.getDeclaredLength());
            }
            return;
        } catch (SecurityException ex) {

        } finally {
            try {
                if (fd != null) {
                    fd.close();
                }
            } catch (IOException ioException) {

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

    private native byte[] _getFrameAtTime(long timeUs, int option);

    public Bitmap getFrameAtTime(long timeUs, int option){
        if(option<OPTION_PREVIOUS_SYNC || option>OPTION_CLOSEST){
            throw new IllegalArgumentException("unsupported option: "+ option);
        }

        Bitmap b=null;

        BitmapFactory.Options bitmapOptionsCache=new BitmapFactory.Options();

        bitmapOptionsCache.inDither=false;

        byte[] picture=_getFrameAtTime(timeUs, option);

        if(picture!=null){
            b=BitmapFactory.decodeByteArray(picture, 0, picture.length, bitmapOptionsCache);
        }

        return b;
    }

    public Bitmap getFrameAtTime(long timeUs){
        Bitmap b= null;

        BitmapFactory.Options bitmapOptionsCache=new BitmapFactory.Options();

        bitmapOptionsCache.inDither=false;

        byte[] picture=_getFrameAtTime(timeUs, OPTION_CLOSEST_SYNC);

        if(picture!=null){
            b=BitmapFactory.decodeByteArray(picture, 0, picture.length, bitmapOptionsCache);
        }

        return b;
    }

    public Bitmap getFrameAtTime(){
        return getFrameAtTime(-1, OPTION_CLOSEST_SYNC);
    }

    public Bitmap getScaledFrameAtTime(long timeUs, int option, int width, int height){
        if(option<OPTION_PREVIOUS_SYNC || option>OPTION_CLOSEST){
            throw new IllegalArgumentException("Unsupported option: "+ option);
        }

        Bitmap b=null;

        BitmapFactory.Options bitmapOptionsCache=new BitmapFactory.Options();

        bitmapOptionsCache.inDither=false;

        byte[] picture=_getScaledFrameAtTime(timeUs, option, widht, height);

        if(picture!=null){
            b=BitmapFactory.decodeByteArray(picture, 0, picture.length, bitmapOptionsCache);
        }

        return b;
    }

    public Bitmap getScaledFrameAtTime(long timeUs, int width, int height){
        Bitmap b=null;

        BitmapFactory.Options bitmapOptionsCache=new BitmapFactory.Options();

        bitmapOptionsCache.inDither=false;

        byte[] picture=_getScaledFrameAtTime(timeUs, OPTION_CLOSEST_SYNC, width, height);

        if(picture!=null){
            b=BitmapFactory.decodeByteArray(picture, 0, picture.length, bitmapOptionsCache);
        }

        return b;
    }

    private native byte[] _getScaledFrameAtTime(long timeUs, int option, int widht, int height);
    public native byte[] getEmbededPicture();

    public native void release();
    private native void native_setup();
    private static native void native_init();

    private native final void native_finalize();

    @Override
    protected void finalize() throws Throwable{
        try{
            native_finalize();
        }finally{
            super.finalize();
        }
    }

    public native void setSurface(Object surface);

    public static final int OPTION_PREVIOUS_SYNC=0x00;

    public static final int OPTION_NEXT_SYNC=0x01;

    public static final int OPTION_CLOSEST_SYNC=0x02;

    public static final int OPTION_CLOSEST= 0x03;

    public static final String METADATA_KEY_ALBUM="album";

    public static final String METADATA_KEY_ALBUM_ARTIST="album_artist";

    public static final String METADATA_KEY_COMMENT="comment";

    public static final String METADATA_KEY_COMPOSER="composer";

    public static final String METADATA_KEY_COPYRIGHT="copyright";

    public static final String METADATA_KEY_CREATION_TIME="creation_time";

    public static final String METADATA_KEY_DATE="date";

    public static final String METADATA_KEY_DISC="disc";

    public static final String METADATA_KEY_ENCODER="encoder";

    public static final String METADATA_KEY_ENCODED_BY="encoded_by";

    public static final String METADATA_KEY_FILENAME="failename";

    public static final String METADATA_KEY_GENRE="genre";

    public static final String METADATA_KEY_LANGUAGE="language";

    public static final String METADATA_KEY_PERFORMER="performer";

    public class Metadata {

        /**
         * {@hide}
         */
        public static final int STRING_VAL=1;

        /**
         * {@hide}
         */
        public static final int INTEGER_VAL=2;

        /**
         * {@hide}
         */
        public static final int BOOLEAN_VAL=3;

        /**
         * {@hide}
         */
        public static final int LONG_VAL=4;

        /**
         * {@hide}
         */
        public static final int DOUBLE_VAL=5;

        /**
         * {@hide}
         */
        public static final int DATE_VAL=6;

        /**
         * {@hide}
         */
        public static final int BYTE_ARRAY_VAL=7;

        private HashMap<String, String> mParcel;
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
        public boolean parse(HashMap<String, String> metadata){
            if(metadata==null){
                return false;
            }else{
                mParcel=metadata;
                return true;
            }
        }

        public boolean has(final String metadataId){
            if(!checkMetadataId(metadataId)){
                throw new IllegalArgumentException("Invalid key: "+ metadataId);
            }
            return mParcel.containsKey(metadataId);
        }

        public HashMap<String, String> getAll(){
            return mParcel;
        }

        /**
         * {@hide}
         */
        public String getString(final String key){
            checkType(key, STRING_VAL);
            return String.valueOf(mParcel.get(key));
        }

        /**
         * {@hide}
         */
        public int getInt(final String key){
            checkType(key, INTEGER_VAL);
            return Integer.valueOf(mParcel.get(key));
        }

        private boolean checkMetadataId(String val){
            return true;
        }

        private void checkType(final String key, final int expectedType){
            String type=mParcel.get(key);

            if(type==null){
                throw new IllegalStateException("Wrong type "+ expectedType + " but got "+ type);
            }
        }
    }

}
