package com.example.xzheng.zmusicplayer;

/**
 * Created by xzheng on 14/12/30.
 */
public class Song {
    private String _url;
    private String _title;
    private String _localFilePath;
    private long _fileSize;
    private String _md5;

    public Song() {

    }

    public String getUrl() {
        return _url;
    }

    public String getTitle() {
        return _title;
    }

    public void setUrl(String url) {
        _url = url;
    }

    public void setTitle(String title) {
        _title = title;
    }

    public String getLocalFilePath() { return _localFilePath;}

    public void setLocalFilePath(String filePath) { _localFilePath = filePath;}

    public long getFileSize() { return _fileSize; }

    public void setFileSize(long size) { _fileSize = size; }

    public String getMD5() { return _md5; }

    public void setMD5(String md5) { _md5 = md5; }


}
