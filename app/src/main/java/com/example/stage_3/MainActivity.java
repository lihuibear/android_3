package com.example.stage_3;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private ImageView nextIv, playIv, lastIv;
    private TextView singerTv, songTv;
    private RecyclerView musicRv;
    private List<LocalMusicBean> mDatas; // 数据源
    private LocalMusicAdapter adapter;
    private int currentPlayPosition = 0;
    private int currentPausePositionInSong = 0;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mediaPlayer = new MediaPlayer();
        mDatas = new ArrayList<>();
        adapter = new LocalMusicAdapter(this, mDatas);
        musicRv.setAdapter(adapter);
        musicRv.setLayoutManager(new LinearLayoutManager(this)); // 设置布局管理器

        // 检查存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        } else {
            loadLocalMusicData(); // 权限已授予，加载音乐数据
        }

        setEventListener(); // 设置点击事件
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadLocalMusicData(); // 权限授予后加载音乐数据
            } else {
                Toast.makeText(this, "存储权限被拒绝，无法访问音乐文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setEventListener() {
        adapter.setOnItemClickListener(new LocalMusicAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {
                currentPlayPosition = position;
                playMusicInMusicPosition(position);
            }
        });

        mediaPlayer.setOnCompletionListener(mediaPlayer -> nextPlay()); // 播放完自动下一首
    }

    private void playMusicInMusicPosition(int position) {
        if (position < 0 || position >= mDatas.size()) return; // 确保有效位置

        LocalMusicBean musicBean = mDatas.get(position);
        singerTv.setText(musicBean.getSinger());
        songTv.setText(musicBean.getSong());
        stopMusic();

        mediaPlayer.reset(); // 重置播放器
        try {
            mediaPlayer.setDataSource(musicBean.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            playIv.setImageResource(R.mipmap.stop);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "无法播放音乐", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            playIv.setImageResource(R.mipmap.play);
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            currentPausePositionInSong = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            playIv.setImageResource(R.mipmap.play);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMusic();
        mediaPlayer.release(); // 释放媒体播放器资源
    }

    private void loadLocalMusicData() {
        ContentResolver resolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = resolver.query(uri, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String song = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String singer = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
                String time = sdf.format(new Date(duration));
                if (!time.equals("00:00")) {
                    LocalMusicBean bean = new LocalMusicBean(String.valueOf(mDatas.size() + 1), song, singer, album, time, path);
                    mDatas.add(bean);
                }
            }
            cursor.close();
        }

        // 更新数据源并通知适配器
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    private void initView() {
        nextIv = findViewById(R.id.local_music_bottom_iv_next);
        playIv = findViewById(R.id.local_music_bottom_iv_play);
        lastIv = findViewById(R.id.local_music_bottom_iv_last);
        singerTv = findViewById(R.id.local_music_bottom_iv_singer);
        songTv = findViewById(R.id.local_music_bottom_iv_song);
        musicRv = findViewById(R.id.local_music_rv);
        nextIv.setOnClickListener(this);
        playIv.setOnClickListener(this);
        lastIv.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.local_music_bottom_iv_last:
                if (currentPlayPosition > 0) {
                    currentPlayPosition--;
                } else {
                    currentPlayPosition = mDatas.size() - 1; // 如果是第一首，跳到最后一首
                }
                playMusicInMusicPosition(currentPlayPosition);
                break;
            case R.id.local_music_bottom_iv_play:
                if (currentPlayPosition == -1) {
                    Toast.makeText(this, "请选择想要播放的音乐", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mediaPlayer.isPlaying()) {
                    pauseMusic();
                } else {
                    playMusicInMusicPosition(currentPlayPosition);
                }
                break;
            case R.id.local_music_bottom_iv_next:
                nextPlay();
                break;
        }
    }

    private void nextPlay() {
        if (currentPlayPosition < mDatas.size() - 1) {
            currentPlayPosition++;
        } else {
            currentPlayPosition = 0; // 如果是最后一首，跳到第一首
        }
        playMusicInMusicPosition(currentPlayPosition);
    }
}
