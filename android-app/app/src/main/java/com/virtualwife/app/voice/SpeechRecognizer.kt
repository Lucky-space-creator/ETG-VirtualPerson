package com.virtualwife.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSpeechRecognizer

class SpeechRecognizer(private val context: Context) {

    private var recognizer: AndroidSpeechRecognizer? = null
    private var onResult: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var onPartialResult: ((String) -> Unit)? = null

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit = {},
        onPartialResult: (String) -> Unit = {}
    ): Boolean {
        if (!AndroidSpeechRecognizer.isRecognitionAvailable(context)) {
            onError("语音识别不可用")
            return false
        }

        this.onResult = onResult
        this.onError = onError
        this.onPartialResult = onPartialResult

        recognizer = AndroidSpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(
                    AndroidSpeechRecognizer.RESULTS_RECOGNITION
                )
                if (!matches.isNullOrEmpty()) {
                    onPartialResult(matches[0])
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(
                    AndroidSpeechRecognizer.RESULTS_RECOGNITION
                )
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                } else {
                    onError("未识别到语音")
                }
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    AndroidSpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音"
                    AndroidSpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音输入超时"
                    AndroidSpeechRecognizer.ERROR_AUDIO -> "音频录制错误"
                    AndroidSpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    AndroidSpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                    else -> "识别错误: $error"
                }
                onError(message)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        recognizer?.startListening(intent)
        return true
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun cancel() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    fun destroy() {
        cancel()
    }
}
