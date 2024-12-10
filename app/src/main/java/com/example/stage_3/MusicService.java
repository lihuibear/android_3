package com.example.stage_3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

        // 播放完成后自动播放下一曲
        mediaPlayer.setOnCompletionListener(mp -> nextMusic());
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

            // 开启前台服务
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

    // 创建通知频道 (适用于 Android O 及以上)
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

    // 返回一个通知
    private Notification getNotification(String songTitle) {
        Intent pauseIntent = new Intent(this, MusicService.class);
        pauseIntent.setAction("ACTION_PAUSE");
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.setAction("ACTION_NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent previousIntent = new Intent(this, MusicService.class);
        previousIntent.setAction("ACTION_PREVIOUS");
        PendingIntent previousPendingIntent = PendingIntent.getService(this, 0, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, "MusicServiceChannel")
                .setContentTitle("正在播放")
                .setContentText(songTitle)
                .setSmallIcon(R.mipmap.ic_launcher) // 替换为你的图标
                .addAction(R.mipmap.last, "上一曲", previousPendingIntent) // 替换为你的图标
                .addAction(isPlaying() ? R.mipmap.stop : R.mipmap.play, isPlaying() ? "暂停" : "播放", pausePendingIntent) // 替换为你的图标
                .addAction(R.mipmap.next, "下一曲", nextPendingIntent) // 替换为你的图标
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    // 处理通知点击事件
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "ACTION_PAUSE":
                    pauseMusic();
                    break;
                case "ACTION_NEXT":
                    nextMusic();
                    break;
                case "ACTION_PREVIOUS":
                    previousMusic();
                    break;
            }
        }
        return START_STICKY; // 表示服务会在被杀死后重启
    }
}
