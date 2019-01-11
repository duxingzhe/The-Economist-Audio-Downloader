package com.theeconomist.downloader.dialog;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.ProgressBar;

import com.theeconomist.downloader.R;
import com.theeconomist.downloader.utils.DownloadUtil;
import com.theeconomist.downloader.utils.FileUtil;

import java.io.File;

import butterknife.BindView;

public class DownloadDialog extends BaseDialog {

    @BindView(R.id.file_operation_progress)
    public ProgressBar fileOperationProgressBar;

    private OnDownloadListener mListener;

    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case UPDATE_DOWNLOAD_PROGRESS:
                    long downloadedSize=msg.getData().getLong("downloadedSize");
                    long totalSize=msg.getData().getLong("totalSize");
                    fileOperationProgressBar.setProgress((int)(downloadedSize/totalSize));
                    break;
                case DISMISS_DIALOG:
                    dismiss();
                    break;
            }
        }
    };

    private final static int UPDATE_DOWNLOAD_PROGRESS=0x1;
    private final static int DISMISS_DIALOG=0x2;

    public DownloadDialog(Context context){
        super(context);
    }

    public DownloadDialog(Context context, int style){
        super(context, style);
    }

    public DownloadDialog(Context context, int style, OnDownloadListener listener){
        super(context, style);
        mListener=listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_operation_dialog);
        downloadFiles();
    }

    private void downloadFiles(){
        if(!TextUtils.isEmpty(FileUtil.url) && !TextUtils.isEmpty(FileUtil.fileName)) {
            DownloadUtil.getInstance().download(FileUtil.url, FileUtil.path, FileUtil.fileName, new DownloadUtil.OnDownloadListener() {

                @Override
                public void onDownloading(int progress, long totalSize, long downloadedSize) {
                    Message msg = new Message();
                    msg.what = UPDATE_DOWNLOAD_PROGRESS;
                    Bundle bundle = new Bundle();
                    bundle.putLong("totalSize", totalSize);
                    bundle.putLong("downloadedSize", downloadedSize);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                }

                @Override
                public void onDownloadSuccess(File file) {
                    mListener.onUnZip(file);
                    mHandler.sendEmptyMessage(DISMISS_DIALOG);
                }

                @Override
                public void onDownloadFailed(Exception e) {
                    mHandler.sendEmptyMessage(DISMISS_DIALOG);
                }
            });
        }else{
            dismiss();
        }
    }

    public interface OnDownloadListener{

        void onUnZip(File file);
    }
}
