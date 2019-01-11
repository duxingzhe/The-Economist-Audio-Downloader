package com.theeconomist.downloader.dialog;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;

import com.theeconomist.downloader.R;
import com.theeconomist.downloader.bean.Mp3FileBean;
import com.theeconomist.downloader.utils.FileUtil;
import com.theeconomist.downloader.utils.MP3Filter;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;

public class AddDialog extends BaseDialog {

    @BindView(R.id.file_operation_progress)
    public ProgressBar fileOperationProgress;

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

    private int totalNum;

    private final static int UPDATE_DELETE_PROGRESS=0x1;
    private final static int DISMISS_DIALOG=0x2;

    public AddDialog(Context context){
        super(context);
    }

    public AddDialog(Context context, int style){
        super(context, style);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_operation_dialog);
        scanningFiles();
    }

    private void scanningFiles(){
        new Thread(){
            @Override
            public void run(){
                ArrayList<Mp3FileBean> mFiles=new ArrayList<>();
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
                    msg.arg1=i;
                    mHandler.sendMessage(msg);
                }

                mHandler.sendEmptyMessage(DISMISS_DIALOG);
            }
        }.run();
    }

}
