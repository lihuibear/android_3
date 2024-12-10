package com.example.stage_3;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private static final String TAG = "MainActivity";

    private ImageView nextIv, playIv, lastIv;
    private TextView singerTv, songTv;
    private RecyclerView musicRv;
    private List<LocalMusicBean> mDatas; // 数据源
    private LocalMusicAdapter adapter;
    private MusicService musicService;
    private boolean isServiceBound = false;

    // 服务连接回调
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isServiceBound = true;
            musicService.setMusicData(mDatas); // 设置音乐数据
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mDatas = new ArrayList<>();
        adapter = new LocalMusicAdapter(this, mDatas);
        musicRv.setAdapter(adapter);
        musicRv.setLayoutManager(new LinearLayoutManager(this));

        // 检查存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_STORAGE_PERMISSION);
        } else {
            loadLocalMusicData(); // 权限已授予，加载音乐数据
        }

        setEventListener(); // 设置点击事件
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 如果是存储权限请求
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                loadLocalMusicData(); // 如果所有权限被授予
            } else {
                Toast.makeText(this, "存储权限被拒绝，无法访问音乐文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setEventListener() {
        adapter.setOnItemClickListener((view, position) -> {
            if (isServiceBound) {
                musicService.playMusic(position); // 调用服务播放音乐
                updateSongInfo(position);
            }
        });

        nextIv.setOnClickListener(this);
        playIv.setOnClickListener(this);
        lastIv.setOnClickListener(this);
    }

    private void updateSongInfo(int position) {
        if (position >= 0 && position < mDatas.size()) {
            LocalMusicBean musicBean = mDatas.get(position);
            singerTv.setText(musicBean.getSinger());
            songTv.setText(musicBean.getSong());
        } else {
            // 处理无效的索引
            singerTv.setText("");
            songTv.setText("");
        }
    }

    private void loadLocalMusicData() {
        ContentResolver resolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI; // 使用外部内容 URI
        Cursor cursor = resolver.query(uri, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String song = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String singer = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                if (path != null) {
                    LocalMusicBean bean = new LocalMusicBean(String.valueOf(mDatas.size() + 1), song, singer, "", "", path);
                    mDatas.add(bean);
                }
            }
            cursor.close();
        } else {
            Log.e(TAG, "无法查询音频文件，Cursor 为 null");
        }

        adapter.notifyDataSetChanged();
        if (isServiceBound) {
            musicService.setMusicData(mDatas); // 通过服务设置音乐数据
        }
    }

    private void initView() {
        nextIv = findViewById(R.id.local_music_bottom_iv_next);
        playIv = findViewById(R.id.local_music_bottom_iv_play);
        lastIv = findViewById(R.id.local_music_bottom_iv_last);
        singerTv = findViewById(R.id.local_music_bottom_iv_singer);
        songTv = findViewById(R.id.local_music_bottom_iv_song);
        musicRv = findViewById(R.id.local_music_rv);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 绑定服务
        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    @Override
    public void onClick(View view) {
        if (!isServiceBound) return;

        switch (view.getId()) {
            case R.id.local_music_bottom_iv_last:
                musicService.previousMusic(); // 上一首
                updateSongInfo(musicService.getCurrentPlayPosition()); // 更新歌曲信息
                break;
            case R.id.local_music_bottom_iv_play:
                if (musicService.isPlaying()) { // 通过服务检查是否在播放
                    musicService.pauseMusic(); // 暂停播放
                    playIv.setImageResource(R.mipmap.play);
                } else {
                    musicService.playMusic(musicService.getCurrentPlayPosition()); // 播放
                    playIv.setImageResource(R.mipmap.stop);
                }
                break;
            case R.id.local_music_bottom_iv_next:
                musicService.nextMusic(); // 下一首
                updateSongInfo(musicService.getCurrentPlayPosition()); // 更新歌曲信息
                break;
        }
    }
}
