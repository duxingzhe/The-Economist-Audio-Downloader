package com.theeconomist.downloader;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.theeconomist.downloader.bean.Mp3FileBean;
import com.theeconomist.downloader.utils.FileUtil;

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
        Mp3FileBean mp3File=mDataList.get(position);
        Glide.with(mContext).load(mp3File.coverImg).apply(RequestOptions.placeholderOf(R.mipmap.file_mp3_icon)).into(viewHolder.mp3FileCoverImg);
        viewHolder.mp3FileTitle.setText(mp3File.name);
        viewHolder.mp3FileDuration.setText(FileUtil.getLongTime(mp3File.duration));
        viewHolder.mp3FileSize.setText(FileUtil.getFileSize(mp3File.fileSize));
    }

    @Override
    public int getItemCount(){
        return mDataList.size();
    }

    public class FileViewHolder extends RecyclerView.ViewHolder{
        public ImageView mp3FileCoverImg;
        public ImageView mp3FilePlayStatus;
        public TextView mp3FileTitle;
        public TextView mp3FileSize;
        public TextView mp3FileDuration;

        public FileViewHolder(View view){
            super(view);

            mp3FileCoverImg=(ImageView)view.findViewById(R.id.cover);
            mp3FilePlayStatus=(ImageView)view.findViewById(R.id.play_status);
            mp3FileTitle=(TextView)view.findViewById(R.id.music_title);
            mp3FileDuration=(TextView)view.findViewById(R.id.music_duration);
            mp3FileSize=(TextView)view.findViewById(R.id.music_size);
        }
    }
}
