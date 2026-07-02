package com.virtualwife.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virtualwife.app.MainApplication
import com.virtualwife.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val rememberMe: Boolean = false,
    val savedUsername: String = "",
    val savedPassword: String = ""
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MainApplication
    private val authRepo = AuthRepository(app.preferencesManager)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _loginSuccess = MutableSharedFlow<Unit>()
    val loginSuccess: SharedFlow<Unit> = _loginSuccess.asSharedFlow()

    init {
        // 监听登录状态
        viewModelScope.launch {
            combine(
                app.preferencesManager.isLoggedIn,
                app.preferencesManager.username
            ) { loggedIn, username ->
                AuthUiState(isLoggedIn = loggedIn, username = username)
            }.collect { state ->
                _uiState.value = state
            }
        }
        // 加载记住我的凭据
        loadSavedCredentials()
    }

    private fun loadSavedCredentials() {
        viewModelScope.launch {
            val rememberMe = authRepo.isRememberMe()
            if (rememberMe) {
                val (username, password) = authRepo.getSavedCredentials()
                _uiState.update {
                    it.copy(rememberMe = true, savedUsername = username, savedPassword = password)
                }
            }
        }
    }

    fun login(username: String, password: String, rememberMe: Boolean = false) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "用户名和密码不能为空") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 保存记住我设置
            authRepo.saveRememberMe(rememberMe, username, password)

            val result = authRepo.login(username, password)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _loginSuccess.emit(Unit)
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "登录失败")
                    }
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
        }
    }

    fun refreshToken() {
        viewModelScope.launch {
            authRepo.refreshToken()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
