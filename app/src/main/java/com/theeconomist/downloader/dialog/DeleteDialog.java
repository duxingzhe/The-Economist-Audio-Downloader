package com.theeconomist.downloader.dialog;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;

import com.theeconomist.downloader.R;
import com.theeconomist.downloader.utils.FileUtil;

import java.io.File;

import butterknife.BindView;

public class DeleteDialog extends BaseDialog {

    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case UPDATE_DELETE_PROGRESS:
                    fileOperationProgress.setProgress(msg.arg1/totalNum);
                    break;
                case DISMISS_DIALOG:
                    dismiss();
                    break;
            }
        }
    };

    @BindView(R.id.file_operation_progress)
    public ProgressBar fileOperationProgress;

    private final static int UPDATE_DELETE_PROGRESS=0x1;
    private final static int DISMISS_DIALOG=0x2;

    private int totalNum;

    public DeleteDialog(Context context){
        super(context);
    }

    public DeleteDialog(Context context, int style){
        super(context, style);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_operation_dialog);
        deletingFiles();
    }

    private void deletingFiles(){
        new Thread(){
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
                    Message msg=new Message();
                    msg.arg1=i;
                    mHandler.sendMessage(msg);
                }

                mHandler.sendEmptyMessage(DISMISS_DIALOG);
            }
        }.start();
    }
}
