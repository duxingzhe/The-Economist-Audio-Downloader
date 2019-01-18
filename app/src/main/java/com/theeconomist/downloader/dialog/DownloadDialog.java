package com.theeconomist.downloader.dialog;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.theeconomist.downloader.R;
import com.theeconomist.downloader.utils.DownloadUtil;
import com.theeconomist.downloader.utils.FileUtil;

import java.io.File;

import butterknife.BindView;

public class DownloadDialog extends BaseDialog {

    @BindView(R.id.file_operation_progress)
    public ProgressBar fileOperationProgressBar;

    private TextView mExitTextView;
    private TextView mDownloadTextView;
    private TextView dialogTitleTextView;
    private TextView progressInfoTextView;

    public DownloadDialog(Context context){
        super(context);
    }

    public DownloadDialog(Context context, int style){
        super(context, style);
    }


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_operation_dialog);

        mDownloadTextView=(TextView)findViewById(R.id.cancel);
        mExitTextView=(TextView)findViewById(R.id.ok);

        mDownloadTextView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                dismiss();
            }
        });

        dialogTitleTextView=(TextView)findViewById(R.id.dialog_title);
        progressInfoTextView=(TextView)findViewById(R.id.progress_info);

        dialogTitleTextView.setText("下载文件中");
    }

    public void setProgress(int progress){
        fileOperationProgressBar.setProgress(progress);
    }
    public void setText(String string){
        progressInfoTextView.setText(string);
    }
}
