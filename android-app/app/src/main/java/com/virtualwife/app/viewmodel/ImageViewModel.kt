package com.virtualwife.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virtualwife.app.MainApplication
import com.virtualwife.app.data.remote.dto.WsImageRecognizeResult
import com.virtualwife.app.data.repository.ImageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ImageUiState(
    val isProcessing: Boolean = false,
    val imageBase64: String? = null,
    val recognizeResult: WsImageRecognizeResult? = null,
    val error: String? = null
)

class ImageViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MainApplication
    private val imageRepo = ImageRepository(application)

    private val _uiState = MutableStateFlow(ImageUiState())
    val uiState: StateFlow<ImageUiState> = _uiState.asStateFlow()

    fun processImageUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            val result = imageRepo.uriToBase64(uri)
            result.fold(
                onSuccess = { base64 ->
                    _uiState.update { it.copy(imageBase64 = base64, isProcessing = false) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(error = e.message, isProcessing = false)
                    }
                }
            )
        }
    }

    fun sendForRecognition(chatViewModel: ChatViewModel, lat: Double? = null, lng: Double? = null) {
        val base64 = _uiState.value.imageBase64 ?: return
        _uiState.update { it.copy(isProcessing = true) }
        chatViewModel.sendImageForRecognition(base64, lat, lng)
    }

    fun onRecognizeResult(result: WsImageRecognizeResult) {
        _uiState.update {
            it.copy(recognizeResult = result, isProcessing = false)
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(recognizeResult = null, imageBase64 = null, error = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
