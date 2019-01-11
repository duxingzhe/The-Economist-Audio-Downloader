package com.theeconomist.downloader;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.theeconomist.downloader.bean.Mp3FileBean;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private View mRootView;
    private Context mContext;
    private List<Mp3FileBean> mDataList;

    public FileAdapter (Context context, List<Mp3FileBean> mData){
        mContext=context;
        mDataList=mData;
    }

    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int position){
        mRootView=LayoutInflater.from(mContext).inflate(R.layout.music_file_item, parent, false);
        FileViewHolder viewHolder=new FileViewHolder(mRootView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(FileViewHolder viewHolder, int position){

    }

    @Override
    public int getItemCount(){
        return mDataList.size();
    }

    public class FileViewHolder extends RecyclerView.ViewHolder{

        public FileViewHolder(View view){
            super(view);
        }
    }
}
