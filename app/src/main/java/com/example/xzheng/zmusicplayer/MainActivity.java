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


public class MainActivity extends Activity {

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _songTitleText = (TextView)findViewById(R.id.songTitleText);
        _progressText = (TextView)findViewById(R.id.progressText);
        _actionButton = (Button)findViewById(R.id.actionButton);

        //_song = createMyOwnSong();
        _song = createExternalSong();

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
            //TODO: implement the pause/resume
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

    private Song createMyOwnSong() {
        Song song = new Song();
        final String songName = "1.mp3";
        song.setUrl(SONG_SERVER_URL + "/" + "files" + "/" + songName);
        song.setLocalFilePath(getSongLocalFilePath(songName));
        song.setTitle(songName);
        return song;
    }

    private Song createExternalSong() {
        Song song = new Song();
        final String songName = "3246857839526428.mp3";
        song.setUrl("http://m1.music.126.net/yyA6KLa7VUYC0EFwVe-fzA==/" + songName);
        song.setLocalFilePath(getSongLocalFilePath(songName));
        song.setTitle(songName);
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

    private void downloadTheSong() {
        final DownloadTask downloadTask = new DownloadTask(this);
        downloadTask.execute(_song);
        setCurrentState(PlayerState.downloading);
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

    private void onSongDownloaded() {
        setCurrentState(PlayerState.downloaded);
    }

    private class DownloadTask extends AsyncTask<Song, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;


        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);

            //update the progress
            String info = progress[0] + "%";
            _progressText.setText(info);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            if (result != null) {
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            } else {
                //Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
                onSongDownloaded();
            }
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
    }
}
