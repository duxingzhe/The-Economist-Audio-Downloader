package com.theeconomist.downloader.utils;

import java.io.File;
import java.io.FileFilter;

public class MP3Filter implements FileFilter {


    @Override
    public boolean accept(File file){
        if(file.getPath().contains("mp3")){
            return true;
        }
        return false;
    }
}
