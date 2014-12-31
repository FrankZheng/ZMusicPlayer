package com.example.xzheng.zmusicplayer;

/**
 * Created by xzheng on 14/12/31.
 */
public interface SongDownloaderHandler {

    public void onSongDownloaded(Song song);

    public void onSongDownloadProgressUpdated(int progress);

    public void onSongDownloadError(String errorInfo);
}
