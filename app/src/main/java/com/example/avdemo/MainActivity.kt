package com.example.avdemo

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import java.io.IOException
import java.util.*


/***
 * 使用 MediaRecorder 录音，使用 MediaPlayer 播放。录音时获取声音的大小，播放时监听是否播放完成，播放完成释放资源。
 *
 * 使用 MediaRecorder 也可实现视频录制。
 */
class MainActivity : AppCompatActivity() {

    private val REQ_RECORD_AUDIO = 100
    private var mStartRecording = true
    private var mStartPlaying = true

    private var mMediaRecorder: MediaRecorder? = null
    var mMediaPlayer: MediaPlayer? = null
    lateinit var mFileName:String

    lateinit var mProgressBar: ProgressBar

    lateinit var btnPlayer: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mProgressBar = findViewById(R.id.progress_bar)

        mFileName = externalCacheDir!!.absolutePath
        mFileName += "/audiorecordtest.3gp"

        AppCenter.start(
            application, "e7e9acff-1ebd-4727-9927-24202114141b", Analytics::class.java, Crashes::class.java
        )

    }

    //使用 MediaRecorder 录制音频
    fun record(view: View) {
        //检查录音权限
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_RECORD_AUDIO)
        } else {
            //已经授权
            onRecord(mStartRecording)
            if (mStartRecording){
                (view as AppCompatButton).text = "Stop recording"
            } else {
                (view as AppCompatButton).text = "Start recording"
            }
            mStartRecording = !mStartRecording
        }
    }

    private fun onRecord(start: Boolean) {
        if (start) {
            startRecording()
            updateMicStatus()
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        mMediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(mFileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException){
                Log.e("Start Recording", "prepare() failed")
            }
            start()
        }
    }

    private fun stopRecording() {
        mMediaRecorder?.apply {
            stop()
            release()
        }
        mMediaRecorder = null
    }

    fun updateMicStatus(){
        val timer = Timer()
        timer.schedule(object:TimerTask(){
            override fun run() {
                mMediaRecorder?.apply {
                    val temp = maxAmplitude
                    println("Max Amplitude $temp")
                    mProgressBar.progress = temp
                }
            }
        },0,200)
    }

    //播放录音
    fun play(view: View) {
        btnPlayer = view as AppCompatButton
        onPlay(mStartPlaying)
        if (mStartPlaying) {
            btnPlayer.text = "Stop playing"
        } else {
            btnPlayer.text = "Start playing"
        }
        mStartPlaying = !mStartPlaying
    }

    private fun onPlay(start: Boolean) {
        if (start) {
            startPlaying()
        } else {
            stopPlaying()
        }
    }

    private fun startPlaying() {
        mMediaPlayer = MediaPlayer().apply {
            setDataSource(mFileName)

            setOnPreparedListener {
                println("Prepared")
            }
            setOnErrorListener { mp, what, extra ->
                println("On Error")
                false
            }

            setOnCompletionListener {
                //播放完成
                println("Play Completion")
                btnPlayer.text = "Start playing"
                mStartPlaying = true
                stopPlaying()
            }
            prepare()
            start()
        }
    }

    private fun stopPlaying() {
        mMediaPlayer?.release()
        mMediaPlayer = null
    }

    override fun onStop() {
        super.onStop()
        stopRecording()
        stopPlaying()
    }

    fun enterVideoPage(view: View) {
        VideoPlayerActivity.intent(this)
    }

    fun enterScreenRecordPage(view: View?) {
        ScreenRecordActivity.intent(this)
    }

}














