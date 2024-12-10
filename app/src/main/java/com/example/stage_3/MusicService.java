package com.example.stage_3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private MediaPlayer mediaPlayer;
    private List<LocalMusicBean> musicData; // 音乐数据列表
    private int currentPosition = 0; // 当前播放音乐的位置
    private boolean isPaused = false; // 标记音乐是否处于暂停状态

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
        createNotificationChannel();

        mediaPlayer.setOnCompletionListener(mp -> nextMusic()); // 播放完成后自动播放下一曲
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
            mediaPlayer.release(); // 释放MediaPlayer资源
            mediaPlayer = null;
        }
    }

    public void setMusicData(List<LocalMusicBean> musicData) {
        this.musicData = musicData; // 设置音乐列表
    }

    public void playMusic(int position) {
        if (musicData == null || position < 0 || position >= musicData.size()) return;

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop(); // 停止当前播放
        }

        try {
            currentPosition = position;
            mediaPlayer.reset(); // 重置MediaPlayer
            mediaPlayer.setDataSource(musicData.get(position).getPath()); // 设置音乐数据源
            mediaPlayer.prepare(); // 准备播放
            mediaPlayer.start(); // 开始播放

            isPaused = false; // 重置暂停状态
            updateNotification(musicData.get(position).getSong()); // 更新通知
            startForeground(1, getNotification(musicData.get(position).getSong())); // 开始前台服务
        } catch (Exception e) {
            Log.e(TAG, "播放音乐失败: " + e.getMessage());
        }
    }

    public void pauseMusic() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause(); // 暂停播放
            isPaused = true; // 更新暂停状态
            updateNotification(musicData.get(currentPosition).getSong()); // 更新通知
        }
    }

    public void resumeMusic() {
        if (isPaused) {
            mediaPlayer.start(); // 恢复播放
            isPaused = false; // 重置暂停状态
            updateNotification(musicData.get(currentPosition).getSong()); // 更新通知
        }
    }

    public void nextMusic() {
        if (musicData != null && currentPosition < musicData.size() - 1) {
            playMusic(currentPosition + 1); // 播放下一曲
        } else if (musicData != null) {
            playMusic(0); // 如果是最后一曲，播放第一曲
        }
    }

    public void previousMusic() {
        if (musicData != null && currentPosition > 0) {
            playMusic(currentPosition - 1); // 播放上一曲
        } else if (musicData != null) {
            playMusic(musicData.size() - 1); // 如果是第一曲，播放最后一曲
        }
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying(); // 返回是否在播放
    }

    public int getCurrentPlayPosition() {
        return currentPosition; // 返回当前播放音乐的位置
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0; // 返回当前播放进度
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0; // 返回音乐总时长
    }

    public void stopPlay() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop(); // 停止播放
        }
    }

    public void seekTo(int progress) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(progress); // 设置播放进度
        }
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "MusicServiceChannel",
                    "Music Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel); // 创建通知渠道
            }
        }
    }

    private Notification getNotification(String songTitle) {
        // 创建暂停按钮的 PendingIntent
        Intent pauseIntent = new Intent(this, MusicService.class);
        pauseIntent.setAction("ACTION_PAUSE");
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE);

        // 创建下一曲按钮的 PendingIntent
        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.setAction("ACTION_NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        // 创建上一曲按钮的 PendingIntent
        Intent previousIntent = new Intent(this, MusicService.class);
        previousIntent.setAction("ACTION_PREVIOUS");
        PendingIntent previousPendingIntent = PendingIntent.getService(this, 0, previousIntent, PendingIntent.FLAG_IMMUTABLE);

        // 创建通知
        return new NotificationCompat.Builder(this, "MusicServiceChannel")
                .setContentTitle("正在播放")
                .setContentText(songTitle)
                .setSmallIcon(R.mipmap.ic_launcher)
                .addAction(R.mipmap.last, "上一曲", previousPendingIntent)
                .addAction(isPlaying() ? R.mipmap.stop : R.mipmap.play, isPlaying() ? "暂停" : "播放", pausePendingIntent)
                .addAction(R.mipmap.next, "下一曲", nextPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String songTitle) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = getNotification(songTitle);
        if (manager != null) {
            manager.notify(1, notification); // 更新通知
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "ACTION_PAUSE":
                    pauseMusic(); // 暂停音乐
                    break;
                case "ACTION_NEXT":
                    nextMusic(); // 下一曲
                    break;
                case "ACTION_PREVIOUS":
                    previousMusic(); // 上一曲
                    break;
            }
        }
        return START_STICKY; // 服务在被杀死后重新创建
    }
}
