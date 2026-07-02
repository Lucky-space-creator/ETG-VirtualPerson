package com.virtualwife.app.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.sqrt

private const val TAG = "TtsPlayer"

class TtsPlayer(private val context: Context) {

    private var player: ExoPlayer? = null
    private var isPlaying = false
    private var onPlaybackComplete: (() -> Unit)? = null
    private var onAmplitudeUpdate: ((Float) -> Unit)? = null

    // 音频振幅监测
    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 平滑振幅值
    private var smoothAmplitude = 0f
    private val amplitudeSmoothing = 0.3f

    fun initialize() {
        player = ExoPlayer.Builder(context).build()
        player?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_ENDED) {
                    isPlaying = false
                    stopAmplitudeMonitoring()
                    onPlaybackComplete?.invoke()
                }
            }
        })
    }

    fun playUrl(url: String, onComplete: (() -> Unit)? = null, onAmplitude: ((Float) -> Unit)? = null) {
        this.onPlaybackComplete = onComplete
        this.onAmplitudeUpdate = onAmplitude

        if (player == null) initialize()

        val mediaItem = MediaItem.fromUri(url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
        isPlaying = true
        startAmplitudeMonitoring()
    }

    fun playBase64(base64Audio: String, onComplete: (() -> Unit)? = null, onAmplitude: ((Float) -> Unit)? = null) {
        this.onPlaybackComplete = onComplete
        this.onAmplitudeUpdate = onAmplitude

        try {
            val audioBytes = android.util.Base64.decode(base64Audio, android.util.Base64.DEFAULT)
            val tempFile = java.io.File(context.cacheDir, "tts_temp_${System.currentTimeMillis()}.wav")
            tempFile.writeBytes(audioBytes)

            if (player == null) initialize()

            val mediaItem = MediaItem.fromUri(Uri.fromFile(tempFile))
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
            isPlaying = true
            startAmplitudeMonitoring()
        } catch (e: Exception) {
            Log.e(TAG, "playBase64 error: ${e.message}")
            onComplete?.invoke()
        }
    }

    /**
     * 开始监测音频振幅
     * 使用模拟方式：基于时间周期生成自然的口型变化
     */
    private fun startAmplitudeMonitoring() {
        stopAmplitudeMonitoring()
        val startTime = System.currentTimeMillis()

        amplitudeJob = scope.launch {
            while (isPlaying) {
                val elapsed = System.currentTimeMillis() - startTime

                // 生成自然的口型振幅
                // 模拟说话节奏：快开慢合
                val cycle = elapsed % 200L  // 每200ms一个音节
                val amplitude = if (cycle < 80) {
                    // 张嘴阶段：快速张开
                    (Math.sin(cycle / 80.0 * Math.PI) * 0.7).toFloat()
                } else {
                    // 闭嘴阶段：缓慢闭合
                    val closeProgress = (cycle - 80) / 120.0
                    (0.7 * (1 - closeProgress) * (1 - closeProgress)).toFloat()
                }

                // 平滑处理
                smoothAmplitude = smoothAmplitude * (1 - amplitudeSmoothing) + amplitude * amplitudeSmoothing

                // 回调振幅值
                onAmplitudeUpdate?.invoke(smoothAmplitude)

                delay(30) // 约33fps
            }
        }
    }

    private fun stopAmplitudeMonitoring() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        smoothAmplitude = 0f
    }

    fun stop() {
        player?.stop()
        isPlaying = false
        stopAmplitudeMonitoring()
    }

    fun pause() {
        player?.pause()
        isPlaying = false
    }

    fun resume() {
        player?.play()
        isPlaying = true
        startAmplitudeMonitoring()
    }

    fun isPlaying(): Boolean = isPlaying

    fun release() {
        stopAmplitudeMonitoring()
        player?.release()
        player = null
        isPlaying = false
    }
}
