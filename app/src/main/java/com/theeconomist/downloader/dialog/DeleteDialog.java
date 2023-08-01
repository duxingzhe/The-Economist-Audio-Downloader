package com.theeconomist.downloader.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.theeconomist.downloader.R;

public class DeleteDialog extends BaseDialog {

    public ProgressBar fileOperationProgressBar;
    private TextView dialogTitleTextView;
    private TextView progressInfoTextView;
    private TextView cancelTextView;

    public DeleteDialog(Context context){
        super(context);
    }

    private OnCancelButtonClickListener mCancelButtonListener;

    public DeleteDialog(Context context, int style){
        super(context, style);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_operation_dialog);

        setCancelable(false);
        fileOperationProgressBar=findViewById(R.id.file_operation_progress);
        cancelTextView=(TextView)findViewById(R.id.cancel);
        dialogTitleTextView=(TextView)findViewById(R.id.dialog_title);
        progressInfoTextView=(TextView)findViewById(R.id.progress_info);

        dialogTitleTextView.setText("删除文件中");

        cancelTextView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                mCancelButtonListener.onCancelButtonClick(view);
                dismiss();
            }
        });
    }

    public void setProgress(int progress){
        fileOperationProgressBar.setProgress(progress);
    }

    public void setText(String string){
        progressInfoTextView.setText(string);
    }

    public void setOnCancelButtonClickListener(OnCancelButtonClickListener mListener){
        mCancelButtonListener=mListener;
    }

    public interface OnCancelButtonClickListener{

        void onCancelButtonClick(View view);
    }
}
