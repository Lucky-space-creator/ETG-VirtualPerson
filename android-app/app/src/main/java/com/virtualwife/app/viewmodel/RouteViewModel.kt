package com.virtualwife.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virtualwife.app.MainApplication
import com.virtualwife.app.data.remote.dto.RouteDto
import com.virtualwife.app.data.remote.dto.SpotDto
import com.virtualwife.app.data.repository.RouteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RouteUiState(
    val routes: List<RouteDto> = emptyList(),
    val selectedRoute: RouteDto? = null,
    val spots: List<SpotDto> = emptyList(),
    val selectedTags: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class RouteViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MainApplication
    private val routeRepo = RouteRepository(app.preferencesManager)

    private val _uiState = MutableStateFlow(RouteUiState())
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedTags = routeRepo.getInterestTags()
            if (savedTags.isNotEmpty()) {
                _uiState.update { it.copy(selectedTags = savedTags) }
            }
        }
    }

    fun loadRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = routeRepo.getRoutes()
            result.fold(
                onSuccess = { routes ->
                    _uiState.update { it.copy(routes = routes, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    fun loadRoutesByTag(tag: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = routeRepo.getRoutesByInterest(tag)
            result.fold(
                onSuccess = { routes ->
                    _uiState.update { it.copy(routes = routes, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    fun selectRoute(route: RouteDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedRoute = route, isLoading = true) }
            val result = routeRepo.getSpotsByRoute(route.id)
            result.fold(
                onSuccess = { spots ->
                    _uiState.update { it.copy(spots = spots, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    fun toggleTag(tag: String) {
        val currentTags = _uiState.value.selectedTags.toMutableSet()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
        } else {
            currentTags.add(tag)
        }
        _uiState.update { it.copy(selectedTags = currentTags) }
        viewModelScope.launch {
            routeRepo.saveInterestTags(currentTags)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedRoute = null, spots = emptyList()) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
