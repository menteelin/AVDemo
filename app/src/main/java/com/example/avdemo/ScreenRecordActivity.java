package com.example.avdemo;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class ScreenRecordActivity extends AppCompatActivity implements View.OnClickListener {
    //请求码
    private final static int REQUEST_CODE = 101;
    //权限请求码
    private final static int PERMISSION_REQUEST_CODE = 1101;
    //录屏工具
    MediaProjectionManager mediaProjectionManager;
    MediaProjection mediaProjection;
    //开始按钮，停止按钮
    Button btn_start_recorder;
    Button btn_stop_recorder;
    //获取录屏范围参数
    DisplayMetrics metrics;
    //录屏服务
    ScreenRecordService screenRecordService;

    Intent screenRecordIntent;

    public static void intent(Activity fromContext){
        Intent intent = new Intent(fromContext,ScreenRecordActivity.class);
        fromContext.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_record);
        //实例化按钮
        btn_start_recorder = findViewById(R.id.btn_start_recorder);
        btn_stop_recorder = findViewById(R.id.btn_stop_recorder);
        //点击按钮，请求录屏
        btn_start_recorder.setOnClickListener(this);
        btn_stop_recorder.setOnClickListener(this);

        screenRecordIntent = new Intent(this,ScreenRecordService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(screenRecordIntent);
        }
    }

    @Override
    //点击事件
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_start_recorder){
            //点击请求录屏后，第一件事，检查权限
            if (!checkPermission()) {
                return;
            }
            //参数传过去以后，如果在录制，提示
            if(screenRecordService != null && screenRecordService.isRunning()){
                Toast.makeText(ScreenRecordActivity.this,"当前正在录屏，请不要重复点击哦！",Toast.LENGTH_SHORT).show();
            } else if(screenRecordService != null && !screenRecordService.isRunning()){
                //没有录制，就开始录制，弹出提示，返回主界面开始录制
                screenRecordService.startRecord();
                //返回主界面开始录制
                setToBackground();
            } else if(screenRecordService == null){
                connectService();
            }
        } else if (id == R.id.btn_stop_recorder){
            if(screenRecordService != null && !screenRecordService.isRunning()){
                //没有在录屏，无法停止，弹出提示
                Toast.makeText(ScreenRecordActivity.this,"您还没有录屏，无法停止，请先开始录屏吧！",Toast.LENGTH_SHORT).show();
            }else if(screenRecordService != null && screenRecordService.isRunning()){
                //正在录屏，点击停止，停止录屏
                screenRecordService.stopRecord();
            }
        }
    }

    //权限检查，连接录屏服务
    public boolean checkPermission() {
        //调用检查权限接口进行权限检查
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},PERMISSION_REQUEST_CODE);
            return false;
        } else {
            //有权限，连接录屏服务，进行录屏
            connectService();
            return true;
        }
    }

    //连接服务
    public void connectService(){
        //通过intent为中介绑定Service，会自动create
        Intent intent = new Intent(this,ScreenRecordService.class);
        //绑定过程连接，选择绑定模式
        bindService(intent,serviceConnection,BIND_AUTO_CREATE);
    }

    //连接服务成功与否，具体连接过程
    //调用连接接口，实现连接，回调连接结果
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //服务连接成功，需要通过Binder获取服务，达到Activity和Service通信的目的
            //获取Binder
            ScreenRecordService.ScreenRecordBinder binder = (ScreenRecordService.ScreenRecordBinder) iBinder;
            //通过Binder获取Service
            screenRecordService = binder.getScreenRecordService();
            //获取到服务，初始化录屏管理者
            mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            //通过管理者，创建录屏请求，通过Intent
            Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
            //将请求码作为标识一起发送，调用该接口，需有返回方法
            startActivityForResult(captureIntent,REQUEST_CODE);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //连接失败
            Toast.makeText(ScreenRecordActivity.this,"录屏服务未连接成功，请重试！",Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    //返回方法，获取返回的信息
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //首先判断请求码是否一致，结果是否ok
        if(requestCode == REQUEST_CODE && resultCode == RESULT_OK){
            //录屏请求成功，使用工具MediaProjection录屏
            //从发送获得的数据和结果中获取该工具
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode,data);
            //将该工具给Service，并一起传过去需要录制的屏幕范围的参数
            if(screenRecordService != null){
                screenRecordService.setMediaProjection(mediaProjection);
                //获取录屏屏幕范围参数
                metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                screenRecordService.setConfig(metrics.widthPixels,metrics.heightPixels,metrics.densityDpi);
            }
        }
    }

    //返回主界面开始录屏，相当于home键
    private void setToBackground(){
        //主页面的Intent
        Intent home = new Intent(Intent.ACTION_MAIN);
        //设置清除栈顶的启动模式
        home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //匹配符号
        home.addCategory(Intent.CATEGORY_HOME);
        //转换界面，隐式匹配，显示调用
        startActivity(home);
    }

    //当应用结束的时候，需要解除绑定服务，防止造成内存泄漏
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null){
            unbindService(serviceConnection);
        }
        if (screenRecordIntent != null){
            stopService(screenRecordIntent);
        }
    }
}
