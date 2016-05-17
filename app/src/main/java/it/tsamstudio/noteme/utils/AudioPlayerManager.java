package it.tsamstudio.noteme.utils;

import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by damiano on 17/05/16.
 */
public class AudioPlayerManager {
    private static final String TAG = "AudioPlayerManager";

    private static AudioPlayerManager instance;

    private MediaPlayer mediaPlayer;
    private boolean isPlaying;
    private boolean isPaused;
    private String path;
    private Runnable seekMonitor;
    private Handler seekMonitorHandler;

    public interface SeekChangeListener {
        void onSeekChanged(int position);
    }

    public interface AudioPlayingListener {
        void onPlayingFinish();
    }

    private SeekChangeListener seekChangeListener;
    private AudioPlayingListener audioPlayingListener;

    private AudioPlayerManager() {
        isPlaying = false;
        isPaused = false;

        seekMonitorHandler = new Handler();
        seekMonitor = new Runnable() {
            @Override
            public void run() {
                if (seekChangeListener != null &&
                        seekMonitorHandler != null &&
                        mediaPlayer != null &&
                        mediaPlayer.isPlaying()) {
                    seekMonitorHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mediaPlayer != null)
                                seekChangeListener.onSeekChanged(mediaPlayer.getCurrentPosition());
                        }
                    });
                    seekMonitorHandler.postDelayed(this, 50);
                }
            }
        };
    }

    public static AudioPlayerManager getInstance() {
        if (instance == null)
            instance = new AudioPlayerManager();
        return instance;
    }

    public AudioPlayerManager init(String path) {
        this.path = path;
        try {
            if (mediaPlayer != null) {
                Log.d(TAG, "init: media player non nullo");
                destroy();
            }
            mediaPlayer = new MediaPlayer();
            FileInputStream fis = new FileInputStream(new File(path));
            mediaPlayer.setDataSource(fis.getFD());
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlaying();
                    if (audioPlayingListener != null) {
                        audioPlayingListener.onPlayingFinish();
                    }
                }
            });
            mediaPlayer.prepare();
        } catch (Exception e) {
            throw new IllegalStateException("Error initializing player. " + e.getMessage());
        }
        return this;
    }

    public void startPlaying() {
        mediaPlayer.start();
        isPlaying = true;
        isPaused = false;
        startSeekMonitoring();
    }

    private void startSeekMonitoring() {
        if (seekMonitorHandler != null) {
            seekMonitorHandler.post(seekMonitor);
        }
    }

    public void pausePlaying() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPaused = true;
            isPlaying = false;
        }
    }

    public void resumePlaying() {
        if (isPaused && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPaused = false;
            isPlaying = true;
            startSeekMonitoring();
        }
    }

    public void stopPlaying() {
        isPlaying = false;
        isPaused = false;
    }

    public void destroy() {
        mediaPlayer.release();
        mediaPlayer = null;
        isPlaying = false;
        isPaused = false;
    }

    public AudioPlayerManager changeSeek(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
        return this;
    }

    public int getAudioDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }

        throw new IllegalStateException("Trying to retrieve audio duration from null media player");
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isStopped() {
        return !isPlaying && !isPaused;
    }

    public static String formatTiming(long currentTime) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(currentTime);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(currentTime) - TimeUnit.MINUTES.toSeconds(minutes);
        // set the current time
        // its ok to show 00:00 in the UI
        return String.format("%02d:%02d", minutes, seconds);
    }

    public AudioPlayerManager setSeekChangeListener(SeekChangeListener seekChangeListener) {
        this.seekChangeListener = seekChangeListener;
        return this;
    }

    public AudioPlayerManager setAudioPlayingListener(AudioPlayingListener audioPlayingListener) {
        this.audioPlayingListener = audioPlayingListener;
        return this;
    }
}
