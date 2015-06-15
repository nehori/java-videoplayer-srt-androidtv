package com.nehori.videoplayer;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnTimedTextListener;
import android.media.MediaPlayer.TrackInfo;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.media.TimedText;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;

public class MainActivity extends Activity implements  SurfaceHolder.Callback,
           OnTimedTextListener,
           MediaPlayerControl {
    private static final String TAG = "TimedTextTest";
    private TextView txtDisplay;
    private SurfaceView mPreview;
    private MediaPlayer mMediaPlayer;
    private MediaController mMediaController;
    private SurfaceHolder mHolder;
    private static Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtDisplay = (TextView) findViewById(R.id.txtDisplay);

        // disable screen-saver
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFormat(PixelFormat.TRANSPARENT);

        mPreview = (SurfaceView) findViewById(R.id.surface);
        mHolder = mPreview.getHolder();
        mHolder.addCallback(this);
        mMediaPlayer = MediaPlayer.create(this, R.raw.video);
        mMediaController = new MediaController(this);
        mMediaController.setMediaPlayer(this);
        mMediaController.setAnchorView(mPreview);

    }

    @SuppressLint("NewApi")
    protected void onResume() {
        super.onResume();
        // allow to continue playing media in the background.
        requestVisibleBehind(true);
    }

    public boolean onDestroy(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onDestroy");

        if (mp != null) {
            mp.release();
            mp = null;
        }

        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder paramSurfaceHolder) {

        try {
            mMediaPlayer.setDisplay(paramSurfaceHolder);
            mMediaPlayer.addTimedTextSource(getSubtitleFile(R.raw.sub),
                                            MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP);
            int textTrackIndex = findTrackIndexFor(
                                     TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT, mMediaPlayer.getTrackInfo());

            if (textTrackIndex >= 0) {
                mMediaPlayer.selectTrack(textTrackIndex);
            } else {
                Log.w(TAG, "Cannot find text track!");
            }

            mMediaPlayer.setOnTimedTextListener(this);
            mMediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder paramSurfaceHolder, int paramInt1,
                               int paramInt2, int paramInt3) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder paramSurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed");

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private int findTrackIndexFor(int mediaTrackType, TrackInfo[] trackInfo) {
        int index = -1;

        for (int i = 0; i < trackInfo.length; i++) {
            if (trackInfo[i].getTrackType() == mediaTrackType) {
                return i;
            }
        }

        return index;
    }

    private String getSubtitleFile(int resId) {
        String fileName = getResources().getResourceEntryName(resId);
        File subtitleFile = getFileStreamPath(fileName);

        if (subtitleFile.exists()) {
            Log.d(TAG, "Subtitle already exists");
            return subtitleFile.getAbsolutePath();
        }

        Log.d(TAG, "Subtitle does not exists, copy it from res/raw");

        // Copy the file from the res/raw folder to your app folder on the
        // device
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = getResources().openRawResource(resId);
            outputStream = new FileOutputStream(subtitleFile, false);
            copyFile(inputStream, outputStream);
            return subtitleFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }

        finally {
            closeStreams(inputStream, outputStream);
        }
        return "";
    }

    private void copyFile(InputStream inputStream, OutputStream outputStream)
    throws IOException {
        final int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];
        int length = -1;

        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
    }

    // A handy method I use to close all the streams
    private void closeStreams(Closeable... closeables) {
        if (closeables != null) {
            for (Closeable stream : closeables) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onTimedText(final MediaPlayer mp, final TimedText text) {
        if (text != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    txtDisplay.setText(text.getText().replaceAll("<.+?>", ""));
                }
            });
        }
    }
    // for MediaController ---------------------------------------
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "KeyCode:" + event.getKeyCode());

        if (event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
            if (!mMediaController.isShowing()) {
                mMediaController.show();
            } else {
                mMediaController.hide();
            }
        }

        return super.dispatchKeyEvent(event);
    }
    @Override
    public void start() {
        mMediaPlayer.start();
    }
    @Override
    public void pause() {
        mMediaPlayer.pause();
    }
    @Override
    public int getDuration() {
        return mMediaPlayer.getDuration();
    }
    @Override
    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }
    @Override
    public void seekTo(int pos) {
        mMediaPlayer.seekTo(pos);
    }
    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }
    @Override
    public int getBufferPercentage() {
        return 0;
    }
    @Override
    public boolean canPause() {
        return true;
    }
    @Override
    public boolean canSeekBackward() {
        return true;
    }
    @Override
    public boolean canSeekForward() {
        return true;
    }
    @Override
    public int getAudioSessionId() {
        return 0;
    }

}
