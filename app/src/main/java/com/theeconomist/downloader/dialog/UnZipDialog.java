package com.theeconomist.downloader.dialog;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;

import com.theeconomist.downloader.R;
import com.theeconomist.downloader.utils.FileUtil;

import butterknife.BindView;

public class UnZipDialog extends BaseDialog {

    @BindView(R.id.file_operation_progress)
    public ProgressBar fileOperationProgressBar;

    private OnUnZipListener mListener;

    private long totalSize;
    private long unZippedSize;

    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case UPDATE_UNZIP_PROGRESS:
                    unZippedSize+=msg.getData().getLong("File Size");
                    fileOperationProgressBar.setProgress((int)(unZippedSize/totalSize));
                    break;
                case DISMISS_DIALOG:
                    mListener.onUnZipSuccess();
                    dismiss();
                    break;
            }
        }
    };

    private final static int UPDATE_UNZIP_PROGRESS=0x1;
    private final static int DISMISS_DIALOG=0x2;

    public UnZipDialog(Context context){
        super(context);
    }

    public UnZipDialog(Context context, int style){
        super(context, style);
    }

    public UnZipDialog(Context context, int style, OnUnZipListener listener){
        super(context, style);
        mListener=listener;
        unZipFile();
    }

    private void unZipFile(){
        if(FileUtil.file!=null&&FileUtil.file.exists()) {
            totalSize = FileUtil.getZipTrueSize(FileUtil.file.getAbsolutePath());
            FileUtil.unZip(FileUtil.file, FileUtil.path, mHandler);
        }
    }

    public interface OnUnZipListener{

        void onUnZipSuccess();
    }
}
