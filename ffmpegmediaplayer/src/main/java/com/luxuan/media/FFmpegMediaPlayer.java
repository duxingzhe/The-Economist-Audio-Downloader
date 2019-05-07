package com.luxuan.media;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FFmpegMediaPlayer {

    public static final boolean METADATA_UPDATE_ONLY=true;

    public static final boolean METADATA_ALL=false;

    public static final boolean APPLY_METADATA_FILTER=true;

    public static final boolean BYPASS_METADATA_FILTER= false;

    private final static String TAG="FFmpegMediaPlayer";

    private static final String[] JNI_LIBRARIES={
        "SDL2",
        "avutil-56",
        "swscale-5",
        "swresample-3",
        "avcodec-58",
        "avformart-58",
        "postproc-55",
        "ssl",
        "ffmpeg_mediaplayer_jni"
    };

    static{
        for(int i=0;i<JNI_LIBRARIES.length;i++){
            System.loadLibrary(JNI_LIBRARIES[i]);
        }

        native_init();
    }

    private final static String IMEDIA_PLAYER="com.luxuan.media.IMediaPlayer";
    private long mNativeContext;
    private int mNativeSurfaceTexture;
    private int mListenerContext;
    private SurfaceHolder mSurfaceHolder;
    private EventHandler mEventHandler;
    private PowerManager.WakeLock mWakeLock=null;
    private boolean mScreenOnWhilePlaying;
    private boolean mStayAwake;

    public FFmpegMediaPlayer(){
        Looper looper;
        if((looper=Looper.myLooper())!=null){
            mEventHandler=new EventHandler(this, looper);
        }else if((looper=Looper.getMainLooper())!=null){
            mEventHandler=new EventHandler(this,looper);
        }else{
            mEventHandler=null;
        }

        native_setup(new WeakReference<FFmpegMediaPlayer>(this));
    }

    private native void _setVideoSurface(Surface surface);

    public Parcel newRequest(){
        Parcel parcel=Parcel.obtain();
        parcel.writeInterfaceToken(IMediaPlayer);
        return parcel;
    }

    public int invoke(Parcel request, Parcel reply){
        int retcode=native_invoke(request, reply);
        reply.setDataPosition(0);
        return retcode;
    }

    public void setDisplay(SurfaceHolder sh){
        mSurfaceHolder=sh;
        Surface surface;
        if(sh!=null){
            surface=sh.getSurface();
        }else{
            surface=null;
        }
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }

    public void setSurface(Surface surface){
        if(mScreenOnWhilePlaying && surface!=null){
            Log.w(TAG,"setScreenOnWhilePlaying(true) is ineffective for Surface");
        }
        mSurfaceHolder=null;
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }

    public static FFmpegMediaPlayer create(Context context, Uri uri){
        return create(context, uri, null);
    }

    public static FFmpegMediaPlayer create(Context context, Uri uri, SurfaceHolder holder){
        try{
            FFmpegMediaPlayer mp=new FFmpegMediaPlayer();
            mp.setDataSource(context, uri);
            if(holder!=null){
                mp.setDisplay(holder);
            }
            mp.prepare();
            return mp;
        }catch(IOException ex){
            Log.d(TAG, "create failed: ", ex);
        }catch(IllegalArgumentException ex){
            Log.d(TAG, "create failed: ", ex);
        }catch(SecurityException ex){
            Log.d(TAG, "create failed: ", ex);
        }

        return null;
    }

    public static FFmpegMediaPlayer create(Context context, int resid){
        try{
            AssetFileDescriptor afd=context.getResources().openRawResourceFd(resid);
            if(afd==null) {
                return null;
            }
            
            FFmpegMediaPlayer mp=new FFmpegMediaPlayer();
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            return mp;
        }catch(IOException ex){
            Log.d(TAG, "create failed: ", ex);
        }catch(IllegalArgumentException ex){
            Log.d(TAG, "create failed: ", ex);
        }catch(SecurityException ex){
            Log.d(TAG, "create failed: ", ex);
        }

        return null;
    }

    public void setDataSource(Context context, Uri uri)
        throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        setDataSource(context, uri, null);
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> headers)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        
        String scheme=uri.getScheme();
        if(scheme==null||scheme.equals("file")){
            setDataSource(uri.getPath());
            return;
        }

        AssetFileDescriptor fd=null;
        try{
            ContentResolver resolver=context.getContentResolver();
            fd=resolver.openAssetFileDescriptor(uri, "r");
            if(fd==null){
                return;
            }

            if(fd.getDeclaredLength()<0){
                setDataSource(fd.getFileDescriptor());
            }else{
                setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getDeclaredLength());
            }
            return;
        }catch(SecurityException ex){
            Log.d(TAG, "create failed: ", ex);
        }catch(IOException ex){
            Log.d(TAG, "create failed: ", ex);
        }finally{
            if(fd!=null){
                fd.close();
            }
        }

        Log.d(TAG,"Couldn't open file on client side, trying server side");
        setDataSource(uri.toString(), headers);
        return;
    }

    public void setDataSource(String path)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException{
        setDataSource(path, null, null);
    }

    public void setDataSource(String path, Map<String, String> headers)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        String[] keys=null;
        String[] values=null;

        if(headers!=null){
            keys=new String[headers.size()];
            values=new String[headers.size()];

            int i=0;
            for(Map.Entry<String, String> entry: headers.entrySet()){
                keys[i]=entry.getKey();
                values[i]=entry.getValue();
                ++i;
            }
        }
        setDataSource(path, keys, values);
    }

    private void setDataSource(String path, String[] keys, String[] values)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        final Uri uri=Uri.parse(path);
        if("file".equals(uri.getScheme())){
            path=uri.getPath();
        }

        final File file=new File(path);
        if(file.exists()){
            _setDataSource(path, keys, values);
        }else{
            _setDataSource(path, keys, values);
        }
    }

    private native void _setDataSource(String path, String[] keys, String[] values)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    public void setDataSource(FileDescriptor fd)
            throws IOException, IllegalArgumentException, IllegalStateException {
        setDataSource(fd, 0, 0x7ffffffffffffffL);
    }

    public void setDataSource(FileDescriptor fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException{
        _setDataSource(fd, offset, length);
    }

    private native void _setDataSource(FileDescriptor fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException;

    public native void prepare() throws IOException, IllegalStateException;

    public native void prepareAsync() throws IllegalStateException;

    public void start() throws IllegalStateException{
        stayAwake(true);
        _start();
    }

    private native void _start() throws IllegalStateException;

    public void stop() throws IllegalStateException{
        stayAwake(false);
        _stop();
    }

    private native void _stop() throws IllegalStateException;

    public void pause() throws IllegalStateException {
        stayAwake(false);
        _pause();
    }

    private native void _pause() throws IllegalStateException;

    public void setWakeMode(Context context, int mode){
        boolean washeld=false;
        if(mWakeLock!=null){
            if(mWakeLock.isHeld()){
                washeld=true;
                mWakeLock.release();
            }
            mWakeLock=null;
        }

        PowerManager pm=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock=pm.newWakeLock(mode|PowerManager.ON_AFTER_RELEASE, FFmpegMediaPlayer.class.getName());
        mWakeLock.setReferenceCounted(false);
        if(washeld){
            mWakeLock.acquire(500);
        }
    }

    public void setScreenOnWhilePlaying(boolean screenOn){
        if(mScreenOnWhilePlaying!=screenOn){
            if(screenOn&&mSurfaceHolder==null){
                Log.w(TAG,"setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder");
            }

            mScreenOnWhilePlaying=screenOn;
            updateSurfaceScreenOn();
        }
    }

    private void stayAwake(boolean awake){
        if(mWakeLock!=null){
            if(awake&&!mWakeLock.isHeld()){
                mWakeLock.acquire(500);
            }else if(!awake&&mWakeLock.isHeld()){
                mWakeLock.release();
            }
        }

        mStayAwake=awake;
        updateSurfaceScreenOn();
    }

    private void updateSurfaceScreenOn(){
        if(mSurfaceHolder!=null){
            mSurfaceHolder.setKeepScreenOn(mScreenOnWhilePlaying && mStayAwake);
        }
    }

    public native int getVideoWidth();

    public native int getVideoHeight();

    public native boolean isPlaying();

    public native void seekTo(int msec) throws IllegalStateException;

    public native int getCurrentPosition();

    public native int getDuration();

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

    public int setMetadataFilter(Set<String> allow, Set<String> block){
        int i=0;

        String[] allowed=new String[allow.size()];
        String[] blocked=new String[block.size()];

        for(String s : allow){
            allowed[i]=s;
            i++;
        }

        i=0;

        for(String s : block){
            blocked[i]=s;
            i++;
        }

        return native_setMetadataFilter(allowed, blocked);
    }

    public native void setNativeMediaPlayer(FFmpegMediaPlayer next);

    public void release(){
        stayAwake(false);
        updateSurfaceScreenOn();
        mOnPreparedListener=null;
        mOnBufferingUpdateListener=null;
        mOnSeekCompleteListener=null;
        mOnErrorListener=null;
        mOnInfoListener=null;
        mOnVideoSizeChangedListener=null;
        mOnTimedTextListener=null;
        _release();
    }

    private native void _release();

    public native void setAudioStreamType(int streamType);

    public native void setLooping(boolean looping);

    public native void setVolume(float leftVolume, float rightVolume);

    public native Bitmap getFrameAt(int mesc) throws IllegalStateException;

    public native void setAudioSessionid(int sessionId) throws IllegalArgumentException, IllegalStateException;

    public native void getAudioSessionId();

    public native void attachAuxEffect(int effectId);

    private static final int KEY_PARAMETER_TIMED_TEXT_TRACK_INDEX=1000;

    private static final int KEY_PARAMETER_TIMED_TEXT_ADD_OUT_OF_BAND_SOURCE=1001;

    public native boolean setParameter(int key, Parcel value);

    public boolean setParam(int key, String value){
        Parcel p=Parcel.obtain();
        p.writeString(value);
        boolean ret=setParameter(key, p);
        p.recycle();
        return ret;
    }

    public boolean setParameter(int key, int value){
        Parcel p=Parcel.obtain();
        p.writeInt(value);
        boolean ret=setParameter(key, p);
        p.recycle();
        return ret;
    }

    private native void getParameter(int key, Parcel reply);

    public Parcel getParcelParameters(int key){
        Parcel p=Parcel.obtain();
        getParameter(key, p);
        return p;
    }

    public String getStringParameter(int key){
        Parcel p=Parcel.obtain();
        getParameter(key, p);
        String ret=p.readString();
        p.recycle();
        return ret;
    }

    public int getIntParameter(int key){
        Parcel p=Parcel.obtain();
        getParameter(key, p);
        int ret=p.readInt();
        p.recycle();
        return ret;
    }

    public native void setAuxEffectSendLevel(float level);

    private native final int native_invoke(Parcel request, Parcel reply);

    private native final HashMap<String, String> native_getMetadata(boolean update_only,
                                                                    boolean apply_filter,
                                                                    HashMap<String, String> reply);
    private native final int native_setMetadataFilter(String[] allowed, String[] blocked);

    private static native final void native_init();
    private native final void native_setup(Object mediaplayer_this);
    private native final void native_finalize();

    public boolean enableTimedTextTrackIndex(int index){
        if(index<0){
            return false;
        }
        return setParameter(KEY_PARAMETER_TIMED_TEXT_TRACK_INDEX, index);
    }

    /**
     * {@hide}
    */
    public boolean enableTimedText(){
        return enableTimedTextTrackIndex(0);
    }

    /**
     * {@hide}
     */
    public boolean disableTimedText(){
        return setParameter(KEY_PARAMETER_TIMED_TEXT_TRACK_INDEX, -1);
    }

    /**
     * {@hide}
     */
    public native static int native_pullBatteryData(Parcel reply);

    @Override
    protected void finalize(){
        native_finalize();
    }

    private static final int MEDIA_NOP=0;
    private static final int MEDIA_PREPARED=1;
    private static final int MEDIA_PLAYBACK_COMPLETE=2;
    private static final int MEDIA_BUFFERING_UPDATE=3;
    private static final int MEDIA_SEEK_COMPLETE=4;
    private static final int MEDIA_SET_VIDEO_SIZE=5;
    private static final int MEDIA_TIMED_TEXT=99;
    private static final int MEDIA_ERROR=100;
    private static final int MEDIA_INFO=200;

    private class EventHandler extends Handler{
        private FFmpegMediaPlayer mMediaPlayer;

        public EventHandler(FFmpegMediaPlayer mp, Looper looper){
            super(looper);
            mMediaPlayer=mp;
        }

        @Override
        public void handleMessage(Message msg) {
             if(mMediaPlayer.mNativeContext==0){
                 Log.w(TAG, "mediaplayer went away with unhandled events");
                 return;
             }

             switch(msg.what){
                 case MEDIA_PREPARED:
                     if(mOnPreparedListener!=null){
                         mOnPreparedListener.onPrepared(mMediaPlayer);
                     }
                     return;
                 case MEDIA_PLAYBACK_COMPLETE:
                     if(mOnCompletionListener!=null) {
                         mOnCompletionListener.onCompletion(mMediaPlayer);
                     }
                     stayAwake(false);
                     return;
                 case MEDIA_BUFFERING_UPDATE:
                     if(mOnBufferingUpdateListener!=null){
                         mOnBufferingUpdateListener.onBufferingUpdate(mMediaPlayer, msg.arg1);
                     }
                     return;
                 case MEDIA_SEEK_COMPLETE:
                     if(mOnSeekCompleteListener!=null){
                         mOnSeekCompleteListener.onSeekComplete(mMediaPlayer);
                     }
                     return;
                 case MEDIA_SET_VIDEO_SIZE:
                     if(mOnVideoSizeChangedListener!=null) {
                         mOnVideoSizeChangedListener.onVideoSizeChanged(mMediaPlayer, msg.arg1, msg.arg2);
                     }
                     return;
                 case MEDIA_ERROR:
                     Log.e(TAG, "Error ("+msg.arg1+","+msg.arg2+")");
                     boolean error_was_handled=false;
                     if(mOnErrorListener!=null){
                         error_was_handled=mOnErrorListener.onError(mMediaPlayer, msg.arg1, msg.arg2);
                     }
                     if(mOnCompletionListener!=null&&!error_was_handled){
                         mOnCompletionListener.onCompletion(mMediaPlayer);
                     }
                     stayAwake(false);
                     return;
                 case MEDIA_INFO:
                     if(msg.arg1!=MEDIA_INFO_VIDEO_TRACK_LAGGING){
                         Log.i(TAG,"Info ("+msg.arg1+","+msg.arg2+")");
                     }
                     if(mOnInfoListener!=null){
                         mOnInfoListener.onInfo(mMediaPlayer, msg.arg1, msg.arg2);
                     }
                     return;
                 case MEDIA_TIMED_TEXT:
                     if(mOnTimedTextListener!=null){
                         if(msg.obj==null){
                             mOnTimedTextListener.onTimedText(mMediaPlayer, null);
                         }else{
                             if(msg.obj instanceof byte[]){
                                 TimedText text=new TimedText((byte[])(msg.obj));
                                 mOnTimedTextListener.onTimedText(mMediaPlayer,text);
                             }
                         }
                     }
                     return;
                 case MEDIA_NOP:
                    break;
                 default:
                     Log.e(TAG,"Unknown message type "+msg.what);
                     return;
             }
        }
    }

    private static void postEventFromNative(Object mediaplayer_ref, int what, int arg1, int arg2, Object obj){
        FFmpegMediaPlayer mp=(FFmpegMediaPlayer)((WeakReference)mediaplayer_ref).get();
        if(mp==null){
            return;
        }

        if(mp.mEventHandler!=null){
            Message m= mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            mp.mEventHandler.sendMessage(m);
        }
    }

    public interface OnPreparedListener{

        void onPrepared(FFmpegMediaPlayer mp);
    }

    public void setOnPreparedListener(OnPreparedListener listener){
        mOnPreparedListener=listener;
    }

    private OnPreparedListener mOnPreparedListener;

    public interface OnCompletionListener{

        void onCompletion(FFmpegMediaPlayer mp);
    }

    public void setOnCompletionListener(OnCompletionListener listener){
        mOnCompletionListener= listener;
    }

    private OnCompletionListener mOnCompletionListener;

    public interface OnBufferingUpdateListener{

        void onBufferingUpdate(FFmpegMediaPlayer mp, int percent);
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener){
        mOnBufferingUpdateListener=listener;
    }

    private OnBufferingUpdateListener mOnBufferingUpdateListener;

    public interface OnSeekCompleteListener{

        void onSeekComplete(FFmpegMediaPlayer mp);
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener listener){
        mOnSeekCompleteListener=listener;
    }

    private OnSeekCompleteListener mOnSeekCompleteListener;

    public interface OnVideoSizeChangedListener{

        void onVideoSizeChanged(FFmpegMediaPlayer mp, int width, int height);
    }

    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener){
        mOnVideoSizeChangedListener=listener;
    }

    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;

    /**
     * {@hide}
     */
    public interface OnTimedTextListener{

        void onTimedText(FFmpegMediaPlayer mp, TimedText text);
    }

    /**
     * {@hide}
     */
    public void setOnTimedTextListener(OnTimedTextListener listener){
        mOnTimedTextListener=listener;
    }

    private OnTimedTextListener mOnTimedTextListener;

    public interface OnErrorListener{

        boolean onError(FFmpegMediaPlayer mp, int what, int extra);
    }

    public void setOnErrorListener(OnErrorListener listener){
        mOnErrorListener=listener;
    }

    private OnErrorListener mOnErrorListener;

    public static final int MEDIA_INFO_UNKNOWN =1;
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING=700;
    public static final int MEDIA_INFO_BUFFERING_START=701;
    public static final int MEDIA_INFO_BUFFERING_END=702;
    public static final int MEDIA_INFO_BAD_INTERLEAVIN=800;
    public static final int MEDIA_INFO_NOT_SEEKABLE=801;
    public static final int MEDIA_INFO_METADATA_UPDATE=802;

    public interface OnInfoListener{

        boolean onInfo(FFmpegMediaPlayer mp, int what, int extra);
    }

    public void setOnInfoListener(OnInfoListener listener){
        mOnInfoListener=listener;
    }

    private OnInfoListener mOnInfoListener;

    private int attachAuxEffectCompat(int effectId){
        int ret=-3;

        return ret;
    }
}
