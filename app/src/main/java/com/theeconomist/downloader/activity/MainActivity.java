package com.theeconomist.downloader.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.theeconomist.downloader.R;
import com.theeconomist.downloader.dialog.AddDialog;
import com.theeconomist.downloader.dialog.DownloadDialog;
import com.theeconomist.downloader.dialog.InputDialog;
import com.theeconomist.downloader.dialog.UnZipDialog;
import com.theeconomist.downloader.utils.FileUtil;

import java.io.File;

import butterknife.BindView;

public class MainActivity extends BaseActivity {

    @BindView(R.id.input)
    public Button inputButton;
    private Button downloadButton;
    @BindView(R.id.unzip)
    public Button unzipButton;
    private RecyclerView recyclerView;

    private Context mContext;

    public final int START_SCANNING_FILE=0x1;

    private Handler handler=new Handler(){

        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case START_SCANNING_FILE:
                    startScanningFile();
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext=this;

        downloadButton=(Button)findViewById(R.id.download);
        recyclerView=(RecyclerView)findViewById(R.id.recyclerview);

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
                DownloadDialog inputDialog=new DownloadDialog(mContext, R.style.StyleDialog, new DownloadDialog.OnDownloadListener() {
                    @Override
                    public void onUnZip(File file) {
                        FileUtil.file=file;
                    }
                });

                inputDialog.show();
            }
        });

        unzipButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view){
                UnZipDialog unZipDialog=new UnZipDialog(mContext, R.style.StyleDialog, new UnZipDialog.OnUnZipListener() {
                    @Override
                    public void onUnZipSuccess() {
                        handler.sendEmptyMessage(START_SCANNING_FILE);
                    }
                });

                unZipDialog.show();
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
    }

    private void startScanningFile(){
        AddDialog addDialog=new AddDialog(mContext, R.style.StyleDialog);
        addDialog.show();
    }
}
