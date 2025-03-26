package com.austinauyeung.nyuma.c9.core.logs.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.austinauyeung.nyuma.c9.core.logs.LogManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LogViewModel : ViewModel() {

    private val _logs = MutableStateFlow<List<LogManager.LogEntry>>(emptyList())
    val logs: StateFlow<List<LogManager.LogEntry>> = _logs.asStateFlow()

    private var logCollectionJob: Job? = null

    init {
        refreshLogs()
    }

    private fun refreshLogs() {
        _logs.value = LogManager.getLogs()
    }

    fun startLogCollection() {
        if (logCollectionJob != null) return

        logCollectionJob = LogManager.logFlow
            .onEach { newEntry ->
                _logs.value = _logs.value + newEntry
            }
            .launchIn(viewModelScope)
    }

    fun stopLogCollection() {
        logCollectionJob?.cancel()
        logCollectionJob = null
    }

    override fun onCleared() {
        stopLogCollection()
        super.onCleared()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LogViewModel::class.java)) {
                return LogViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}