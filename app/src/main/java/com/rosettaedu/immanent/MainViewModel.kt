package com.rosettaedu.immanent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rosettaedu.immanent.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val preferencesRepository: PreferencesRepository) : ViewModel() {

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen = _isFullScreen.asStateFlow()

    val imageUrl = preferencesRepository.imageUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onFullScreenEntered() {
        _isFullScreen.value = true
    }

    fun onFullScreenExited() {
        _isFullScreen.value = false
    }

    fun updateImageUrl(imageUrl: String) {
        viewModelScope.launch {
            preferencesRepository.updateImageUrl(imageUrl)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val preferencesRepository =
                    (this[APPLICATION_KEY] as ImmanentApplication).preferencesRepository
                MainViewModel(preferencesRepository = preferencesRepository)
            }
        }
    }
}
