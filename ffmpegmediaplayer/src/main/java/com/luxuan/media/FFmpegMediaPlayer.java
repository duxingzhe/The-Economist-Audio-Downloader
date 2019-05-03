package com.luxuan.media;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Looper;
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
        }else if((looper=Looper.getMainLooper(this, looper))!=NULL){
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

    public static FFmepegMediaPlayer create(Context context, Uri uri){
        return create(context, uri, null);
    }

    public static FFmpegMediaPlayer create(Context context, Uri uri, SurfaceHolder holder){
        try{
            FFmpegMediaPlayer mp=new FFmpegMediaPlayer();
            mp.setDataSource(context, uri);
            if(holder!=null){
                mp.setDisplay(hodler);
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

    public void stop() throws IllegalStateexception{
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

}
