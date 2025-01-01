package com.rosettaedu.immanent

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class MainViewModel : ViewModel() {

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen = _isFullScreen.asStateFlow()


    fun onFullScreenEntered() {
        _isFullScreen.value = true
    }

    fun onFullScreenExited() {
        _isFullScreen.value = false
    }

}
