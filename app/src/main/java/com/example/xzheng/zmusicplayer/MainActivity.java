package com.example.xzheng.zmusicplayer;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends Activity implements SongDownloaderHandler {

    private static final String LOG_TAG = "MainActivity";
    private static final String SONG_SERVER_URL = "http://ec2-54-169-150-55.ap-southeast-1.compute.amazonaws.com:3001";

    private enum PlayerState {
        initialize,
        downloading,
        downloading_paused,
        downloaded,
        playing,
        stopped,
    };

    private PlayerState _currentState = PlayerState.initialize;

    private TextView _progressText;
    private Button _actionButton;
    private TextView _songTitleText;
    private MediaPlayer _mediaPlayer;
    private Song _song;
    private SongDownloader _songDownloader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _songTitleText = (TextView)findViewById(R.id.songTitleText);
        _progressText = (TextView)findViewById(R.id.progressText);
        _actionButton = (Button)findViewById(R.id.actionButton);

        _song = createMyOwnSong();
        //_song = createExternalSong();

        //setup ui controls
        _songTitleText.setText(_song.getUrl());
        configActionButton();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActionButtonClick(View view) {
        Log.d(LOG_TAG, "onActionButtonClick " + getCurrentState());
        PlayerState state = getCurrentState();

        if(state == PlayerState.initialize) {
            Log.d(LOG_TAG, "download the song");
            downloadTheSong();
        } else if(state == PlayerState.downloading) {
            Log.d(LOG_TAG, "pause the downloading");
            pauseTheSongDownloading();
        } else if(state == PlayerState.downloading_paused) {
            Log.d(LOG_TAG, "resume the downloading");
            //TODO: implement the pause/resume
        } else if(state == PlayerState.downloaded || state == PlayerState.stopped) {
            Log.d(LOG_TAG, "play the song");
            playTheSong();
        } else if(state == PlayerState.playing) {
            Log.d(LOG_TAG, "stop the song");
            stopTheSong();
        } else {
            Log.e(LOG_TAG, "unsupported state " + _currentState);
        }

    }

    @Override
    public void onSongDownloaded(Song song) {
        setCurrentState(PlayerState.downloaded);
    }

    @Override
    public void onSongDownloadProgressUpdated(int progress) {
        _progressText.setText(progress + "%");
    }

    @Override
    public void onSongDownloadError(String errorInfo) {
        Toast.makeText(this, "Download error: " + errorInfo, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSongDownloadPaused() {
        setCurrentState(PlayerState.downloading_paused);
    }

    private Song createMyOwnSong() {
        Song song = new Song();
        final String songName = "1.mp3";
        song.setUrl(SONG_SERVER_URL + "/" + "files" + "/" + songName);
        song.setLocalFilePath(getSongLocalFilePath(songName));
        song.setTitle(songName);
        song.setFileSize(8434209);
        song.setMD5("300043aa2014bf07c1018ef946b274a2");
        return song;
    }

    private Song createExternalSong() {
        Song song = new Song();
        final String songName = "3246857839526428.mp3";
        song.setUrl("http://m1.music.126.net/yyA6KLa7VUYC0EFwVe-fzA==/" + songName);
        song.setLocalFilePath(getSongLocalFilePath(songName));
        song.setTitle(songName);
        song.setFileSize(8125145);
        song.setMD5("63eba7a4548e7557b37d9c0573a3f175");
        return song;
    }

    private String getSongLocalFilePath(String filename) {
        return Environment.getExternalStorageDirectory() + "/" + filename;
    }

    private MediaPlayer getMediaPlayer() {
        if(_mediaPlayer == null) {
            _mediaPlayer = new MediaPlayer();
        }
        return _mediaPlayer;
    }

    private SongDownloader getSongDownloader() {
        if(_songDownloader == null) {
            _songDownloader = new SongDownloader(_song, this, this);
        }
        return _songDownloader;
    }

    private void downloadTheSong() {
        SongDownloader downloader = getSongDownloader();
        downloader.start();
        setCurrentState(PlayerState.downloading);
    }

    private void pauseTheSongDownloading() {
        SongDownloader downloader = getSongDownloader();
        downloader.pause();
    }

    private void playTheSong() {
        MediaPlayer mediaPlayer = getMediaPlayer();
        String filePath = _song.getLocalFilePath();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            setCurrentState(PlayerState.playing);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e1) {
            e1.printStackTrace();
        }
    }

    private void stopTheSong() {
        MediaPlayer mediaPlayer = getMediaPlayer();
        mediaPlayer.stop();
        mediaPlayer.reset();
        setCurrentState(PlayerState.stopped);
    }

    private PlayerState getCurrentState() {
        return _currentState;
    }

    private void setCurrentState(PlayerState state) {
        _currentState = state;
        configActionButton();
    }

    private void configActionButton() {
        String actionTitle = "";
        PlayerState state = getCurrentState();

        //configure button text with different state
        if(state == PlayerState.initialize) {
            actionTitle = "Download";
        } else if(state == PlayerState.downloading) {
            actionTitle = "Pause";
        } else if(state == PlayerState.downloading_paused) {
            actionTitle = "Resume";
        } else if(state == PlayerState.downloaded || state == PlayerState.stopped) {
            actionTitle = "Play";
        } else if(state == PlayerState.playing) {
            actionTitle = "Stop";
        } else {
            Log.e(LOG_TAG, "unknown state: " + state);
        }
        // TODO: string to to be localized
        _actionButton.setText(actionTitle);
    }


}
