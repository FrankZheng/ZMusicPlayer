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


    private TextView _progressText;
    private Button _actionButton;
    private TextView _songTitleText;
    private int _currentState = 0;
    private MediaPlayer _mediaPlayer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _songTitleText = (TextView)findViewById(R.id.songTitleText);
        _progressText = (TextView)findViewById(R.id.progressText);
        _actionButton = (Button)findViewById(R.id.actionButton);

        //setup ui controls
        _songTitleText.setText(getSongUrl());
        _actionButton.setText("Download");


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
        Log.d(LOG_TAG, "onActionButtonClick " + _currentState);
        if(_currentState == 0) {
            Log.d(LOG_TAG, "download the song");
            final DownloadTask downloadTask = new DownloadTask(this);
            final String songUrl = getSongUrl();
            downloadTask.execute(songUrl);
            setCurrentState(1);
        } else if(_currentState == 1) {
            Log.d(LOG_TAG, "pause the downloading");
            //TODO: implement the pause/resume
        } else if(_currentState == 2) {
            Log.d(LOG_TAG, "resume the downloading");
            //TODO: implement the pause/resume
        } else if(_currentState == 3) {
            Log.d(LOG_TAG, "play the song");
            playTheSong();
            setCurrentState(4);
        } else if(_currentState == 4) {
            Log.d(LOG_TAG, "stop the song");
            stopTheSong();
            setCurrentState(3);
        } else {
            Log.e(LOG_TAG, "unsupported state " + _currentState);
        }

    }

    private String getSongUrl() {
        final String songFolder = "files";
        final String songFileName = "1.mp3";
        return SONG_SERVER_URL + "/" + songFolder + "/" + songFileName;
    }

    private String getSongLocalFilePath() {
        return Environment.getExternalStorageDirectory() + "/" + "1.mp3";
    }

    private MediaPlayer getMediaPlayer() {
        if(_mediaPlayer == null) {
            _mediaPlayer = new MediaPlayer();
        }
        return _mediaPlayer;
    }

    private void playTheSong() {
        MediaPlayer mediaPlayer = getMediaPlayer();
        String filePath = getSongLocalFilePath();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
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
    }

    private int getCurrentState() {
        return _currentState;
    }
    private void setCurrentState(int state) {
        _currentState = state;
        configActionButton();
    }

    private void configActionButton() {
        String actionTitle = "";
        if(_currentState == 0) {
            actionTitle = "Download";
        } else if(_currentState == 1) {
            actionTitle = "Pause";
        } else if(_currentState == 2) {
            actionTitle = "Resume";
        } else if(_currentState == 3) {
            actionTitle = "Play";
        } else if(_currentState == 4) {
            actionTitle = "Stop";
        }
        _actionButton.setText(actionTitle);
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

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
            //mProgressDialog.show();
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
                //update the state
                setCurrentState(3);
            }
        }


        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
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
                String filePath = getSongLocalFilePath();
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