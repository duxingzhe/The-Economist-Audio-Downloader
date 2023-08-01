package com.theeconomist.downloader.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.theeconomist.downloader.R;

public class AddDialog extends BaseDialog {

    public ProgressBar fileOperationProgress;

    private TextView cancelTextView;
    private TextView dialogTitleTextView;
    private TextView progressInfoTextView;

    private OnCancelButtonClickListener mCancelButtonListener;

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

        fileOperationProgress=findViewById(R.id.file_operation_progress);
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

        dialogTitleTextView.setText("添加文件中");

    }

    public void setProgress(int progress){
        fileOperationProgress.setProgress(progress);
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
