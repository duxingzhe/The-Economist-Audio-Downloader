package com.theeconomist.downloader.dialog;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

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

    private TextView cancelTextView;

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

        cancelTextView=(TextView)findViewById(R.id.cancel);
        cancelTextView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                dismiss();
            }
        });

    }

    public void setProgress(int progress){
        fileOperationProgress.setProgress(progress);
    }

}
