package com.example.avdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;


public class VideoPlayerActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    private AppCompatButton mBtnPlay;
    private AppCompatButton mBtnPause;
    private AppCompatButton mBtnStop;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private MediaPlayer mMediaPlayer;

    private boolean mPrepared = false;
    private boolean mFirstEnter = true;

    public static void intent(Context context){
        Intent intent = new Intent(context,VideoPlayerActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player_activity);

        mBtnPlay = findViewById(R.id.btn_play);
        mBtnPause = findViewById(R.id.btn_pause);
        mBtnStop = findViewById(R.id.btn_stop);

        mBtnPlay.setOnClickListener(this);
        mBtnPause.setOnClickListener(this);
        mBtnStop.setOnClickListener(this);


        //方式1，同步准备
        mMediaPlayer = MediaPlayer.create(this,R.raw.oceans);
        //是否循环播放
        mMediaPlayer.setLooping(false);
        int duration = mMediaPlayer.getDuration();
        System.out.println("duration:" + duration / 1000  + " s");
        mMediaPlayer.setOnPreparedListener(mp -> {
            System.out.println("Media Player Prepared.");
            mPrepared = true;
            if (mFirstEnter){
                mFirstEnter = false;
            } else {
                mp.start();
            }
        });

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Toast.makeText(VideoPlayerActivity.this,"播放完了",Toast.LENGTH_LONG).show();
            }
        });

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e("VideoPlayer","播放遇到错误:what:"+what + ",extra:"+extra);
                return false;
            }
        });

        mSurfaceView = findViewById(R.id.surface_view);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(VideoPlayerActivity.this);

    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        System.out.println("Surface Created");
        //下面这两个方法都可以
//        mMediaPlayer.setDisplay(holder);
        mMediaPlayer.setSurface(holder.getSurface());
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_play){
            if (mPrepared){
                mMediaPlayer.start();
            } else {
                mMediaPlayer.prepareAsync();
            }
        } else if (id == R.id.btn_pause){
            if (mMediaPlayer.isPlaying()){
                mMediaPlayer.pause();
            }
        } else if (id == R.id.btn_stop){
            if (mMediaPlayer.isPlaying()){
                mMediaPlayer.stop();
                mPrepared = false;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer.isPlaying()){
            mMediaPlayer.stop();
        }
        mMediaPlayer.release();
    }
}














