package com.example.xzheng.zmusicplayer;

/**
 * Created by xzheng on 14/12/30.
 */
public class Song {
    private String _url;
    private String _title;
    private String _localFilePath;

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


}
