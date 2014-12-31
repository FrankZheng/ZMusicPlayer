package com.example.xzheng.zmusicplayer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.widget.Toast;

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
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();

            // download the file
            input = connection.getInputStream();
            String filePath = song.getLocalFilePath();
            output = new FileOutputStream(filePath);

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                // allow canceling with back button
                if (isCancelled()) {
                    input.close();
                    return null;
                }
                total += count;
                // publishing the progress....
                if (fileLength > 0) // only if total length is known
                    publishProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
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
