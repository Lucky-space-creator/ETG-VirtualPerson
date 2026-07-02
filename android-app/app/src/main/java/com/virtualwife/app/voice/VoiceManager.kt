package com.virtualwife.app.voice

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class VoiceManager(private val context: Context) {

    private val audioRecorder = AudioRecorder(context)
    private val speechRecognizer = SpeechRecognizer(context)
    val ttsPlayer = TtsPlayer(context)

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recognizedText = MutableSharedFlow<String>()
    val recognizedText: SharedFlow<String> = _recognizedText.asSharedFlow()

    enum class RecordingState {
        IDLE, RECORDING, PROCESSING
    }

    fun startVoiceInput() {
        _recordingState.value = RecordingState.RECORDING

        speechRecognizer.startListening(
            onResult = { text ->
                _recordingState.value = RecordingState.IDLE
                CoroutineScope(Dispatchers.Main).launch {
                    _recognizedText.emit(text)
                }
            },
            onError = { _ ->
                _recordingState.value = RecordingState.IDLE
            },
            onPartialResult = { }
        )
    }

    fun stopVoiceInput() {
        _recordingState.value = RecordingState.PROCESSING
        speechRecognizer.stopListening()
    }

    fun playTtsAudio(audioUrl: String, onComplete: (() -> Unit)? = null, onAmplitude: ((Float) -> Unit)? = null) {
        if (audioUrl.startsWith("base64:")) {
            val base64 = audioUrl.removePrefix("base64:")
            ttsPlayer.playBase64(base64, onComplete, onAmplitude)
        } else {
            ttsPlayer.playUrl(audioUrl, onComplete, onAmplitude)
        }
    }

    fun stopTts() {
        ttsPlayer.stop()
    }

    fun release() {
        audioRecorder.release()
        speechRecognizer.destroy()
        ttsPlayer.release()
    }
}
