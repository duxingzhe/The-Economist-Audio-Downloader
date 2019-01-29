package com.theeconomist.downloader.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.theeconomist.downloader.FileAdapter;
import com.theeconomist.downloader.R;
import com.theeconomist.downloader.bean.EventBusBean;
import com.theeconomist.downloader.bean.Mp3FileBean;
import com.theeconomist.downloader.dialog.AddDialog;
import com.theeconomist.downloader.dialog.DeleteDialog;
import com.theeconomist.downloader.dialog.DownloadDialog;
import com.theeconomist.downloader.dialog.InputDialog;
import com.theeconomist.downloader.dialog.UnZipDialog;
import com.theeconomist.downloader.utils.DownloadUtil;
import com.theeconomist.downloader.utils.EventType;
import com.theeconomist.downloader.utils.FileUtil;
import com.theeconomist.downloader.utils.MP3Filter;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;

public class MainActivity extends BaseMusicActivity {

    @BindView(R.id.input)
    public Button inputButton;
    private Button deleteButton;
    private Button exitButton;
    private RecyclerView recyclerView;
    private FileAdapter mAdapter;

    private LinearLayout bottomPlayStatusLayout;
    private Context mContext;

    private EventBusBean eventNextBean;

    // 文件总数
    private int totalNum;
    // 压缩包总大小
    private long totalSize;
    // 已解压大小
    private long unZippedSize;

    // Handler Message标识
    private final static int START_SCANNING_FILE=0x1;
    private final static int UPDATE_ADD_FILE_PROGRESS=0x2;
    private final static int DISMISS_ADD_FILE_DIALOG=0x3;
    public final static int UPDATE_UNZIP_PROGRESS=0x4;
    public final static int DISMISS_UNZIP_DIALOG=0x5;
    private final static int UPDATE_DOWNLOAD_PROGRESS=0x6;
    private final static int DISMISS_DOWNLOAD_DIALOG=0x7;
    private final static int UPDATE_DELETE_PROGRESS=0x8;
    private final static int DISMISS_DELETE_DIALOG=0x9;
    private final static int START_DOWNLOADING_FILE=0x10;
    private final static int START_UNZIPPING_FILE=0x11;

    private static final String ACTION_MEDIA_SCANNER_SCAN_DIR = "android.intent.action.MEDIA_SCANNER_SCAN_DIR";

    private AddDialog addDialog;
    private UnZipDialog unZipDialog;
    private DownloadDialog downloadDialog;
    private DeleteDialog deleteDialog;

    // 三个线程，用于解压、删除和扫描文件
    private Thread unZipThread, deleteThread, scanFileThread;

    ArrayList<Mp3FileBean> mFiles=new ArrayList<>();

    private Handler handler=new Handler(){

        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case START_SCANNING_FILE:
                    startScanningFile();
                    break;
                case UPDATE_ADD_FILE_PROGRESS:
                    addDialog.setText("已添加" + (msg.arg1 + 1) + "个文件，共" + totalNum + "个");
                    addDialog.setProgress((msg.arg1+1)*100/totalNum);
                    break;
                case DISMISS_ADD_FILE_DIALOG:
                    addDialog.setText("添加完成");
                    addDialog.dismiss();
                    notifyDataChanged();
                    break;
                case UPDATE_UNZIP_PROGRESS:
                    unZippedSize+=msg.getData().getLong("File Size");
                    unZipDialog.setProgressInfoText("已解压"+FileUtil.getFileSize(unZippedSize)+"，共"+
                            FileUtil.getFileSize(totalSize));
                    unZipDialog.setProgress((int)(unZippedSize*100/totalSize));
                    break;
                case DISMISS_UNZIP_DIALOG:
                    unZipDialog.dismiss();
                    handler.sendEmptyMessageDelayed(START_SCANNING_FILE,500);
                    break;
                case UPDATE_DOWNLOAD_PROGRESS:
                    long downloadedSize=msg.getData().getLong("downloadedSize");
                    long totalSize=msg.getData().getLong("totalSize");
                    downloadDialog.setText("已下载"+FileUtil.getFileSize(downloadedSize)+"，共"+
                            FileUtil.getFileSize(totalSize));
                    downloadDialog.setProgress((int)(downloadedSize*100/totalSize));
                    break;
                case DISMISS_DOWNLOAD_DIALOG:
                    downloadDialog.dismiss();
                    handler.sendEmptyMessageDelayed(START_UNZIPPING_FILE,500);
                    break;
                case UPDATE_DELETE_PROGRESS:
                    deleteDialog.setText("已删除" + (msg.arg1 + 1) + "个文件，共" + totalNum + "个");
                    deleteDialog.setProgress(msg.arg1*100/totalNum);
                    break;
                case DISMISS_DELETE_DIALOG:
                    deleteDialog.dismiss();
                    handler.sendEmptyMessageDelayed(START_SCANNING_FILE,500);
                    break;
                case START_DOWNLOADING_FILE:
                    downloadFile();
                    break;
                case START_UNZIPPING_FILE:
                    unzipFile();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext=this;

        mAdapter=new FileAdapter(mContext, mFiles);
        recyclerView=(RecyclerView)findViewById(R.id.recyclerview);
        bottomPlayStatusLayout=(LinearLayout)findViewById(R.id.ly_mini_player);
        deleteButton=(Button)findViewById(R.id.delete);
        exitButton=(Button)findViewById(R.id.exit);

        inputButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                InputDialog inputDialog=new InputDialog(mContext, R.style.StyleDialog, new InputDialog.OnDownloadListener() {
                    @Override
                    public void downloadFile(String downloadUrl, String fileName) {
                        FileUtil.url=downloadUrl;
                        FileUtil.fileName=fileName;
                        handler.sendEmptyMessageDelayed(START_DOWNLOADING_FILE,500);
                    }
                });

                inputDialog.show();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                deleteFile();
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        bottomPlayStatusLayout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                startActivity(MainActivity.this, PlayerActivity.class);
            }
        });

        mAdapter.setOnItemClickListener(new FileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Mp3FileBean mp3FileBean, int position) {
                getPlayBean().setName(mp3FileBean.name);
                getPlayBean().setImg(mp3FileBean.coverImg);
                getPlayBean().setUrl(mp3FileBean.path);
                getPlayBean().setIndex(mp3FileBean.index);
                getPlayBean().setDuration((int)mp3FileBean.duration);
                getPlayBean().setAblumName(mp3FileBean.albumName);
                startActivity(MainActivity.this, PlayerActivity.class);
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        startScanningFile();
    }

    private void startScanningFile(){
        addDialog=new AddDialog(mContext, R.style.StyleDialog);
        addDialog.show();

        if(mFiles.size()>0){
            mFiles.clear();
        }

        addDialog.setOnCancelButtonClickListener(new AddDialog.OnCancelButtonClickListener() {
            @Override
            public void onCancelButtonClick(View view) {
                if(scanFileThread.isAlive()){
                    scanFileThread.interrupt();
                }
            }
        });

        scanFileThread=new Thread(){
            @Override
            public void run(){

                File file=new File(FileUtil.path);

                if(!file.exists()){
                    return;
                }
                // 获取MP3文件
                File[] filteredFiles=file.listFiles(new MP3Filter());

                // 文件总数
                totalNum=filteredFiles.length;

                Intent scanIntent = new Intent(ACTION_MEDIA_SCANNER_SCAN_DIR);
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
                    Uri mediaFileUri = FileProvider.getUriForFile(mContext, "com.theeconomist.downloader.fileprovider", file);
                    scanIntent.setData(mediaFileUri);
                }else {
                    scanIntent.setData(Uri.fromFile(file));
                }
                mContext.sendBroadcast(scanIntent);

                for(int i=0;i<totalNum;i++){
                    File mp3SingleFile=filteredFiles[i];
                    Mp3FileBean mp3File=new Mp3FileBean(mp3SingleFile.getAbsolutePath());
                    // 如果加载失败，使用其他方法
                    if(!FileUtil.getMusicInfo(mContext,mp3File)){
                        FileUtil.loadMP3Info(mp3File);
                    }
                    mp3File.index=i;
                    mFiles.add(mp3File);
                    Message msg=new Message();
                    msg.what=UPDATE_ADD_FILE_PROGRESS;
                    msg.arg1=i;
                    handler.sendMessage(msg);
                    try {
                        Thread.sleep(200);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }

                handler.sendEmptyMessageDelayed(DISMISS_ADD_FILE_DIALOG,1000);
            }
        };

        scanFileThread.start();
    }

    private void notifyDataChanged(){
        FileUtil.fileList=mFiles;
        recyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        handler.removeCallbacks(null);
    }

    @Override
    public void onMusicStatus(int status) {
        super.onMusicStatus(status);
        switch (status) {
            case PLAY_STATUS_RESUME:
                break;
            case PLAY_STATUS_COMPLETE:
                playNextMusic();
                break;
            default:
                break;
        }
    }

    private void deleteFile(){
        deleteDialog=new DeleteDialog(mContext, R.style.StyleDialog);
        deleteDialog.show();

        deleteDialog.setOnCancelButtonClickListener(new DeleteDialog.OnCancelButtonClickListener(){

            @Override
            public void onCancelButtonClick(View view){
                if(deleteThread.isAlive()){
                    deleteThread.interrupt();
                }
            }
        });

        deleteThread=new Thread(){
            @Override
            public void run(){
                File file=new File(FileUtil.path);

                if(!file.exists()){
                    return;
                }
                // 获取MP3文件
                File[] filteredFiles=file.listFiles();
                // 总文件数量
                totalNum=filteredFiles.length;
                for(int i=0;i<totalNum;i++){
                    File mp3File=filteredFiles[i];
                    mp3File.delete();
                    FileUtil.deleteMusicFile(mContext, mp3File.getPath());
                    Message msg=new Message();
                    msg.what=UPDATE_DELETE_PROGRESS;
                    msg.arg1=i;
                    handler.sendMessageDelayed(msg,500);
                    try {
                        Thread.sleep(200);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }

                handler.sendEmptyMessageDelayed(DISMISS_DELETE_DIALOG, 1000);
            }
        };

        deleteThread.start();
    }

    private void downloadFile(){
        if(!TextUtils.isEmpty(FileUtil.url)) {
            downloadDialog = new DownloadDialog(mContext, R.style.StyleDialog);

            downloadDialog.show();

            downloadDialog.setOnCancelButtonClickListener(new DownloadDialog.OnCancelButtonClickListener() {
                @Override
                public void onCancelButtonClick(View view) {
                    DownloadUtil.getInstance().cancel();
                }
            });

            if(!TextUtils.isEmpty(FileUtil.url) && !TextUtils.isEmpty(FileUtil.fileName)) {
                DownloadUtil.getInstance().download(FileUtil.url, FileUtil.path, FileUtil.fileName, new DownloadUtil.OnDownloadListener() {

                    @Override
                    public void onDownloading(long totalSize, long downloadedSize) {
                        Message msg = new Message();
                        msg.what = UPDATE_DOWNLOAD_PROGRESS;
                        Bundle bundle = new Bundle();
                        bundle.putLong("totalSize", totalSize);
                        bundle.putLong("downloadedSize", downloadedSize);
                        msg.setData(bundle);
                        handler.sendMessageDelayed(msg,500);
                        try {
                            Thread.sleep(200);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onDownloadSuccess(File file) {
                        FileUtil.file = file;
                        handler.sendEmptyMessageDelayed(DISMISS_DOWNLOAD_DIALOG,1000);
                    }

                    @Override
                    public void onDownloadFailed(Exception e) {
                        handler.sendEmptyMessageDelayed(DISMISS_DOWNLOAD_DIALOG,1000);
                    }
                });
            }else{
                downloadDialog.dismiss();
            }
        }
    }

    private void unzipFile(){
        if(FileUtil.file!=null&&FileUtil.file.exists()) {

            unZipDialog = new UnZipDialog(mContext, R.style.StyleDialog);
            unZipDialog.show();

            unZipDialog.setOnCancelButtonClickListener(new UnZipDialog.OnCancelButtonClickListener() {
                @Override
                public void onCancelButtonClick(View view) {
                    if(unZipThread.isAlive()){
                        unZipThread.interrupt();
                    }
                }
            });

            if(FileUtil.file!=null&&FileUtil.file.exists()) {
                totalSize = FileUtil.getZipTrueSize(FileUtil.file.getAbsolutePath());
                unZipThread=new Thread(){
                    @Override
                    public void run(){
                        FileUtil.unZip(FileUtil.file, FileUtil.path,handler);
                    }
                };
                unZipThread.start();
            }
        }
    }

    @Override
    public void playNextMusic(){
        playNext(true);
    }

    @Override
    public void playMusic(){
        if(!TextUtils.isEmpty(getPlayBean().getUrl())) {
            if (!getPlayBean().getUrl().equals(playUrl)) {
                setCdRadio(0f);
                if (eventNextBean == null) {
                    eventNextBean = new EventBusBean(EventType.MUSIC_NEXT, getPlayBean().getUrl());
                } else {
                    eventNextBean.setType(EventType.MUSIC_NEXT);
                    eventNextBean.setObject(getPlayBean().getUrl());
                }
                EventBus.getDefault().post(eventNextBean);
                playUrl = getPlayBean().getUrl();
                getTimeBean().setTotalSecs(getPlayBean().getDuration());
                getTimeBean().setCurrSecs(0);
            }
        }
        initMiniBar();
    }

    private void initMiniBar() {
        if(ivMiniBg != null) {
            Glide.with(this).load(getPlayBean().getImgByte()).apply(RequestOptions.errorOf(R.mipmap.file_mp3_icon)).into(ivMiniBg);
        }
        if(tvMiniName != null) {
            if(!tvMiniName.getText().toString().trim().equals(getPlayBean().getName())) {
                tvMiniName.setText(getPlayBean().getName());
            }
        }
        if(tvMiniSubName != null) {
            if(TextUtils.isEmpty(getPlayBean().getAlbumName())) {
                tvMiniSubName.setText("The Economist");
            }else{
                tvMiniSubName.setText("The Economist - "+getPlayBean().getAlbumName());
            }
        }
    }

}
