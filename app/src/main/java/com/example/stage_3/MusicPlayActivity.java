package com.example.stage_3;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MusicPlayActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView ivMusic;
    private SeekBar sb;
    private TextView tvProgress, tvTotal;
    private Button btnPlay, btnPause, btnContinuePlay, btnExit;
    private MusicService musicService;
    private boolean isServiceBound = false;
    private ObjectAnimator animator;
    private Handler handler = new Handler();
    private Runnable updateRunnable;

    // 服务连接回调
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isServiceBound = true;

            // 更新UI，获取当前音乐信息
            updateUI();
            startUpdatingSeekBar();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            stopUpdatingSeekBar();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_play);

        initView();
        initAnimator();

        // 绑定服务
        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void initView() {
        ivMusic = findViewById(R.id.iv_music);
        sb = findViewById(R.id.sb);
        tvProgress = findViewById(R.id.tv_progress);
        tvTotal = findViewById(R.id.tv_total);
        btnPlay = findViewById(R.id.btn_play);
        btnPause = findViewById(R.id.btn_pause);
        btnContinuePlay = findViewById(R.id.btn_continue_play);
        btnExit = findViewById(R.id.btn_exit);

        btnPlay.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnContinuePlay.setOnClickListener(this);
        btnExit.setOnClickListener(this);

        // SeekBar的进度改变监听
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isServiceBound) {
                    musicService.seekTo(progress); // 调整音乐播放进度
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (animator != null) {
                    animator.pause(); // 暂停旋转动画
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (animator != null) {
                    animator.start(); // 继续旋转动画
                }
            }
        });
    }

    private void initAnimator() {
        animator = ObjectAnimator.ofFloat(ivMusic, "rotation", 0f, 360f);
        animator.setDuration(10000); // 动画旋转一周的时间为10秒
        animator.setRepeatCount(ObjectAnimator.INFINITE); // 无限循环
        animator.start(); // 启动动画
    }

    private void updateUI() {
        if (musicService != null) {
            sb.setMax(musicService.getDuration()); // 设置SeekBar最大值
            sb.setProgress(musicService.getCurrentPosition()); // 设置SeekBar当前进度

            tvTotal.setText(formatTime(musicService.getDuration())); // 设置总时长
            tvProgress.setText(formatTime(musicService.getCurrentPosition())); // 设置当前播放时长
        }
    }

    private String formatTime(int time) {
        int minutes = (time / 1000) / 60;
        int seconds = (time / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds); // 格式化为 mm:ss
    }

    private void startUpdatingSeekBar() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isServiceBound && musicService.isPlaying()) {
                    updateUI(); // 更新UI
                }
                handler.postDelayed(this, 1000); // 每秒更新一次
            }
        };
        handler.post(updateRunnable); // 启动更新
    }

    private void stopUpdatingSeekBar() {
        handler.removeCallbacks(updateRunnable); // 停止更新
    }

    @Override
    public void onClick(View v) {
        if (!isServiceBound) return;

        switch (v.getId()) {
            case R.id.btn_play:
                musicService.playMusic(0); // 播放第一首音乐
                break;
            case R.id.btn_pause:
                musicService.pauseMusic(); // 暂停音乐
                btnPause.setEnabled(false); // 禁用暂停按钮
                btnContinuePlay.setEnabled(true); // 启用继续播放按钮
                if (animator != null) {
                    animator.pause(); // 暂停旋转动画
                }
                break;
            case R.id.btn_continue_play:
                musicService.resumeMusic(); // 继续播放音乐
                btnPause.setEnabled(true); // 启用暂停按钮
                btnContinuePlay.setEnabled(false); // 禁用继续播放按钮
                if (animator != null) {
                    animator.start(); // 继续旋转动画
                }
                break;
            case R.id.btn_exit:
                unbindService(serviceConnection);
                stopUpdatingSeekBar(); // 停止更新
                finish(); // 退出当前Activity
                break;
        }
        updateUI(); // 更新UI显示
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        stopUpdatingSeekBar(); // 停止更新
    }
}
