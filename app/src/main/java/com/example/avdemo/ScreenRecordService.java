package com.example.avdemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class ScreenRecordService extends Service {

    public static String TAG = "ScreenRecordActivity";
    public static final String NOTIFICATION_CHANNEL_ID = UUID.randomUUID().toString();
    public static final String NOTIFICATION_CHANNEL_NAME = "ScreenRecordChannel";
    public static final int NOTIFICATION_ID = 100;

    //录屏工具MediaProjection
    private MediaProjection mediaProjection;
    //录像机MediaRecorder
    private MediaRecorder mediaRecorder;
    //用于录屏的虚拟屏幕
    private VirtualDisplay virtualDisplay;
    //声明录制屏幕的宽高像素
    private int width;
    private int height;
    private int dpi;
    //标志，判断是否正在录屏
    private boolean running;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG,"onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG,"onStartCommand");
        notification();
        return super.onStartCommand(intent, flags, startId);
    }

    public void notification() {
        Log.i(TAG, "notification: " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Call Start foreground with notification
            Intent notificationIntent = new Intent(this, ScreenRecordActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground))
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Capture Screen")
                    .setContentText("The Service Capture Screen is Running.")
                    .setContentIntent(pendingIntent);
            Notification notification = notificationBuilder.build();

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
            //notificationManager.notify(NOTIFICATION_ID, notification);
            startForeground(NOTIFICATION_ID, notification); //必须使用此方法显示通知，不能使用notificationManager.notify，否则还是会报上面的错误
        }
    }


    @Nullable
    @Override
    //返回一个Binder用于通信，需要一个获取Service的方法
    public IBinder onBind(Intent intent) {
        Log.e(TAG,"onBind");
        return new ScreenRecordBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG,"onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG,"onDestroy");
    }

    //返回的Binder
    public class ScreenRecordBinder extends Binder {
        //返回Service的方法
        public ScreenRecordService getScreenRecordService() {
            return ScreenRecordService.this;
        }
    }

    //设置录屏工具MediaProjection
    public void setMediaProjection(MediaProjection projection) {
        mediaProjection = projection;
    }

    //设置需要录制的屏幕参数
    public void setConfig(int width, int height, int dpi) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }

    //返回判断，判断其是否在录屏
    public boolean isRunning() {
        return running;
    }

    //服务的两个主要逻辑
    //开始录屏
    public boolean startRecord() {
        //首先判断是否有录屏工具以及是否在录屏
        if (mediaProjection == null || running) {
            return false;
        }
        //有录屏工具，没有在录屏，就进行录屏
        //初始化录像机，录音机Recorder
        initRecorder();
        //根据获取的屏幕参数创建虚拟的录屏屏幕
        createVirtualDisplay();
        //本来不加异常也可以，但是这样就不知道是否start成功
        //万一start没有成功，但是running置为true了，就产生了错误也无提示
        //提示开始录屏了，但是并没有工作
        try{
            //准备工作都完成了，可以开始录屏了
            mediaRecorder.start();
            //标志位改为正在录屏
            running = true;
            return true;
        }catch (Exception e){
            e.printStackTrace();
            //有异常，start出错，没有开始录屏，弹出提示
            Toast.makeText(this,"开启失败，没有开始录屏",Toast.LENGTH_SHORT).show();
            //标志位变回没有录屏的状态
            running = false;
            return false;
        }
    }

    //停止录屏
    public boolean stopRecord() {
        if (!running) {
            //没有在录屏，无法停止
            return false;
        }
        //无论设备是否还原或者有异常，但是确实录屏结束，修改标志位为未录屏
        running = false;
        //本来加不加捕获异常都可以，但是为了用户体验度，加入会更好
        try{
            //Recorder停止录像，重置还原，以便下一次使用
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            //释放virtualDisplay的资源
            virtualDisplay.release();
        }catch (Exception e){
            e.printStackTrace();
            //有异常，保存失败，弹出提示
            Toast.makeText(this,"录屏出现异常，视频保存失败！",Toast.LENGTH_SHORT).show();
            return false;
        }
        //无异常，保存成功
        Toast.makeText(this,"录屏结束，保存成功！",Toast.LENGTH_SHORT).show();
        return true;
    }

    //初始化Recorder录像机
    public void initRecorder() {
        //新建Recorder
        mediaRecorder = new MediaRecorder();
        //设置录像机的一系列参数
        //设置音频来源
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //设置视频来源
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        //设置视频格式为mp4
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //设置视频存储地址，返回的文件夹下的命名为当前系统事件的文件
        String videoPath = getSaveDirectory() + System.currentTimeMillis() + ".mp4";
        //保存在该位置
        mediaRecorder.setOutputFile(videoPath);
        //设置视频大小，清晰度
        mediaRecorder.setVideoSize(width, height);
        //设置视频编码为H.264
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //设置音频编码
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        //设置视频码率
        mediaRecorder.setVideoEncodingBitRate(2 * width * height);
        mediaRecorder.setVideoFrameRate(24);
        //初始化完成，进入准备阶段，准备被使用
        //截获异常，处理
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            //异常提示
            Toast.makeText(this,
                    "Recorder录像机prepare失败，无法使用，请重新初始化！",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void createVirtualDisplay() {
        //虚拟屏幕通过MediaProjection获取，传入一系列传过来的参数
        //可能创建时会出错，捕获异常
        try {
            virtualDisplay = mediaProjection.createVirtualDisplay("VirtualScreen", width, height, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this,"virtualDisplay创建录屏异常，请退出重试！",Toast.LENGTH_SHORT).show();
        }
    }


    //获取存储文件夹的位置
    public String getSaveDirectory() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //如果确认为视频类型，设置根目录，绝对路径下的自定义文件夹中
            String rootDir = getExternalCacheDir().getAbsolutePath() + "/" + "captures" + "/";
            //创建该文件夹
            File file = new File(rootDir);
            if (!file.exists()) {
                //如果该文件夹不存在
                if (!file.mkdirs()) {
                    //如果没有创建成功
                    return null;
                }
            }
            //创建成功了，返回该目录
            return rootDir;
        } else {
            //不是音视频文件，不保存，无路径
            return null;
        }
    }
}
