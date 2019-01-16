package com.theeconomist.downloader.dialog;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.theeconomist.downloader.R;

public class InputDialog extends BaseDialog {

    private EditText issueEditText;
    private EditText yearEditText;
    private EditText monthEditText;
    private EditText dayEditText;

    private TextView mExitTextView;
    private TextView mDownloadTextView;

    private OnDownloadListener mListener;

    public InputDialog(Context context){
        super(context);
    }

    public InputDialog(Context context, int styleId){
        super(context, styleId);
    }

    public InputDialog(Context context, int styleId, OnDownloadListener listener){
        super(context,styleId);
        mListener=listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_dialog_layout);

        issueEditText=(EditText)findViewById(R.id.issue);
        yearEditText=(EditText)findViewById(R.id.year);
        monthEditText=(EditText)findViewById(R.id.month);
        dayEditText=(EditText)findViewById(R.id.day);

        mDownloadTextView=(TextView)findViewById(R.id.download);
        mExitTextView=(TextView)findViewById(R.id.exit);

        mDownloadTextView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(!TextUtils.isEmpty(issueEditText.getText().toString())
                        || !TextUtils.isEmpty(yearEditText.getText().toString())
                        || !TextUtils.isEmpty(monthEditText.getText().toString())
                        || !TextUtils.isEmpty(dayEditText.getText().toString())){

                    String day, month;

                    if(monthEditText.getText().toString().length()<=1){
                        month = "0" + monthEditText.getText().toString();
                    }else{
                        month = monthEditText.getText().toString();
                    }

                    if(dayEditText.getText().toString().length()<=1){
                        day="0"+dayEditText.getText().toString();
                    }else{
                        day=dayEditText.getText().toString();
                    }

                    String date=yearEditText.getText().toString()+month+day;
                    String downloadUrl="https://audiocdn.economist.com/sites/default/files/AudioArchive/" +
                            yearEditText.getText().toString() + "/" + date + "/Issue_"+issueEditText.getText().toString() + "_" + date +"_The_Economist_Full_edition.zip";
                    String fileName="Issue_"+issueEditText.getText().toString() + "_" + date +"_The_Economist_Full_edition.zip";

                    mListener.downloadFile(downloadUrl,fileName);
                    dismiss();
                }
            }
        });

        mExitTextView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                dismiss();
            }
        });
    }

    // 回调传到Activity中处理
    public interface OnDownloadListener{

        // 传字符串到另一个Dialog中下载
        void downloadFile(String downloadUrl, String fileName);
    }
}
