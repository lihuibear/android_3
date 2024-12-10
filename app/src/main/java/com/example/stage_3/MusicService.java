package com.example.stage_3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private MediaPlayer mediaPlayer;
    private List<LocalMusicBean> musicData;
    private int currentPosition = 0;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        createNotificationChannel(); // 创建通知频道
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void setMusicData(List<LocalMusicBean> musicData) {
        this.musicData = musicData;
    }

    public void playMusic(int position) {
        if (musicData == null || position < 0 || position >= musicData.size()) return;

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        try {
            currentPosition = position;
            mediaPlayer.reset();
            mediaPlayer.setDataSource(musicData.get(position).getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            startForeground(1, getNotification(musicData.get(position).getSong())); // 显示通知
        } catch (Exception e) {
            Log.e(TAG, "播放音乐失败: " + e.getMessage());
        }
    }

    public void pauseMusic() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void nextMusic() {
        if (musicData != null && currentPosition < musicData.size() - 1) {
            playMusic(currentPosition + 1);
        }
    }

    public void previousMusic() {
        if (musicData != null && currentPosition > 0) {
            playMusic(currentPosition - 1);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public int getCurrentPlayPosition() {
        return currentPosition;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "MusicServiceChannel",
                    "Music Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification(String songTitle) {
        return new NotificationCompat.Builder(this, "MusicServiceChannel")
                .setContentTitle("正在播放")
                .setContentText(songTitle)
                .setSmallIcon(R.mipmap.ic_launcher) // 替换为你的图标
                .build();
    }
}
