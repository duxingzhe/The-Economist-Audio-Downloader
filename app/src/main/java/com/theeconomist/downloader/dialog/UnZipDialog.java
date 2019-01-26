package com.theeconomist.downloader.dialog;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.theeconomist.downloader.R;
import com.theeconomist.downloader.utils.FileUtil;

import butterknife.BindView;

public class UnZipDialog extends BaseDialog {

    @BindView(R.id.file_operation_progress)
    public ProgressBar fileOperationProgressBar;
    private TextView cancelTextView;
    private TextView dialogTitleTextView;
    private TextView progressInfoTextView;

    private OnCancelButtonClickListener mCancelButtonListener;

    public void setOnCancelButtonClickListener(OnCancelButtonClickListener mListener){
        mCancelButtonListener=mListener;
    }

    public UnZipDialog(Context context){
        super(context);
    }

    public UnZipDialog(Context context, int style){
        super(context, style);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_operation_dialog);

        setCancelable(false);
        cancelTextView=(TextView)findViewById(R.id.cancel);
        dialogTitleTextView=(TextView)findViewById(R.id.dialog_title);
        progressInfoTextView=(TextView)findViewById(R.id.progress_info);
        cancelTextView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                mCancelButtonListener.onCancelButtonClick(view);
                dismiss();
            }
        });

        dialogTitleTextView.setText("解压文件中");
    }

    public void setProgress(int progress){
        fileOperationProgressBar.setProgress(progress);
    }

    public void setProgressInfoText(String str){
        progressInfoTextView.setText(str);
    }

    public interface OnCancelButtonClickListener{

        void onCancelButtonClick(View view);
    }
}
