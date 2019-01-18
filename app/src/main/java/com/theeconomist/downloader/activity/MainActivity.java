package com.theeconomist.downloader.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.theeconomist.downloader.FileAdapter;
import com.theeconomist.downloader.R;
import com.theeconomist.downloader.bean.Mp3FileBean;
import com.theeconomist.downloader.dialog.AddDialog;
import com.theeconomist.downloader.dialog.DownloadDialog;
import com.theeconomist.downloader.dialog.InputDialog;
import com.theeconomist.downloader.dialog.UnZipDialog;
import com.theeconomist.downloader.utils.FileUtil;
import com.theeconomist.downloader.utils.MP3Filter;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;

public class MainActivity extends BaseActivity {

    @BindView(R.id.input)
    public Button inputButton;
    private Button downloadButton;
    private Button scanButton;
    @BindView(R.id.unzip)
    public Button unzipButton;
    private RecyclerView recyclerView;
    private FileAdapter mAdapter;

    private LinearLayout bottomPlayStatusLayout;
    private Context mContext;

    // 文件总数
    private int totalNum;
    // 压缩包总大小
    private long totalSize;
    // 已解压大小
    private long unZippedSize;

    private final static int START_SCANNING_FILE=0x1;
    private final static int UPDATE_ADD_FILE_PROGRESS=0x2;
    private final static int DISMISS_ADD_FILE_DIALOG=0x3;
    public final static int UPDATE_UNZIP_PROGRESS=0x4;
    public final static int DISMISS_UNZIP_DIALOG=0x5;

    private AddDialog addDialog;
    private UnZipDialog unZipDialog;

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
        downloadButton=(Button)findViewById(R.id.download);
        recyclerView=(RecyclerView)findViewById(R.id.recyclerview);
        bottomPlayStatusLayout=(LinearLayout)findViewById(R.id.ly_status);
        scanButton=(Button)findViewById(R.id.scan);

        inputButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                InputDialog inputDialog=new InputDialog(mContext, R.style.StyleDialog, new InputDialog.OnDownloadListener() {
                    @Override
                    public void downloadFile(String downloadUrl, String fileName) {
                        FileUtil.url=downloadUrl;
                        FileUtil.fileName=fileName;
                    }
                });

                inputDialog.show();
            }
        });

        downloadButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view){

                if(!TextUtils.isEmpty(FileUtil.url)) {
                    DownloadDialog inputDialog = new DownloadDialog(mContext, R.style.StyleDialog, new DownloadDialog.OnDownloadListener() {
                        @Override
                        public void onUnZip(File file) {
                            FileUtil.file = file;
                        }
                    });

                    inputDialog.show();
                }
            }
        });

        unzipButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view){
                if(FileUtil.file!=null&&FileUtil.file.exists()) {
                    unZipDialog = new UnZipDialog(mContext, R.style.StyleDialog);
                    unZipDialog.show();
                    if(FileUtil.file!=null&&FileUtil.file.exists()) {
                        totalSize = FileUtil.getZipTrueSize(FileUtil.file.getAbsolutePath());
                        new Thread(){
                            @Override
                            public void run(){
                                FileUtil.unZip(FileUtil.file, FileUtil.path,handler);
                            }
                        }.start();
                    }
                }
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                startScanningFile();
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

        recyclerView.setAdapter(mAdapter);
    }

    private void startScanningFile(){
        addDialog=new AddDialog(mContext, R.style.StyleDialog);
        addDialog.show();

        new Thread(){
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

                for(int i=0;i<totalNum;i++){
                    File mp3File=filteredFiles[i];
                    mFiles.add(new Mp3FileBean(mp3File.getAbsolutePath()));
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
        }.start();
    }

    private void notifyDataChanged(){
        mAdapter.notifyDataSetChanged();
    }
}
