package com.example.xzheng.zmusicplayer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by xzheng on 14/12/31.
 */
public class SongDownloader extends AsyncTask<Song, Integer, String> {
    private static final String TAG = "SongDownloader";

    private Context _context;
    private SongDownloaderHandler _downloadHandler;
    private Song _song;
    private PowerManager.WakeLock _wakeLock;

    public SongDownloader(Song song, SongDownloaderHandler downloadHandler, Context context) {
        _context = context;
        _downloadHandler = downloadHandler;
        _song = song;
    }

    public void start() {
        execute(_song);
    }

    public void pause() {
        //cancel the download
        this.cancel(true);
    }

    public void resume() {
        //start a new one
        //however, need some work to check the existed size
    }

    @Override
    protected void onCancelled(String result) {
        super.onCancelled();
        _downloadHandler.onSongDownloadPaused();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        PowerManager pm = (PowerManager) _context.getSystemService(Context.POWER_SERVICE);
        _wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        _wakeLock.acquire();
    }

    @Override
    protected String doInBackground(Song... songs) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            Song song = songs[0];
            URL url = new URL(song.getUrl());
            connection = (HttpURLConnection) url.openConnection();

            String fileLocalPath = song.getLocalFilePath();
            File file = new File(fileLocalPath);
            long existsLength = 0;

            if(file.exists()) {
                long length = file.length();
                Log.i(TAG, "file exists, length is " + length);
                if(length < song.getFileSize()) {
                    //we start download from last position
                    existsLength = length;
                    connection.setRequestProperty("Range", "bytes=" + existsLength + "-");
                    output = new FileOutputStream(file, true);
                }

            }

            connection.connect();

            // expect HTTP 200 OK or 206 partial content, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK &&
                    connection.getResponseCode() != HttpURLConnection.HTTP_PARTIAL) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            //int fileLength = connection.getContentLength();

            // download the file
            input = connection.getInputStream();

            if(output == null) {
                output = new FileOutputStream(file);
            }

            byte data[] = new byte[4096];
            long total = existsLength;
            int count;
            while ((count = input.read(data)) != -1) {
                // allow canceling with back button
                if (isCancelled()) {
                    input.close();
                    return null;
                }
                total += count;
                // publishing the progress....
                publishProgress((int) (total * 100 / song.getFileSize()));
                output.write(data, 0, count);
            }
            //String md5 = MD5.calculateMD5(file);
            if(!MD5.checkMD5(song.getMD5(), file)) {
                return "Mismatched MD5";
            }

        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        _downloadHandler.onSongDownloadProgressUpdated(progress[0]);
    }


    @Override
    protected void onPostExecute(String result) {
        _wakeLock.release();
        if (result != null) {
            //Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            _downloadHandler.onSongDownloadError(result);
        } else {
            //Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
            _downloadHandler.onSongDownloaded(_song);
        }
    }
}
