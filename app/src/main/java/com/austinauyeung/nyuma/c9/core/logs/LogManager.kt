package com.austinauyeung.nyuma.c9.core.logs

import com.austinauyeung.nyuma.c9.C9
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object LogManager {
    private const val LOG_MAX_SIZE = 500
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    data class LogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val level: Logger.Level,
        val message: String,
        val tag: String? = null
    ) {
        fun formattedTimestamp(): String = dateFormat.format(Date(timestamp))
    }

    fun addLog(level: Logger.Level, message: String, tag: String? = null) {
        val settings = C9.getInstance().getSettingsFlow().value

        if (settings.collectLogs) {
            val entry = LogEntry(level = level, message = message, tag = tag)

            _logs.update { currentLogs ->
                if (currentLogs.isNotEmpty()) {
                    currentLogs.drop(max(currentLogs.size - LOG_MAX_SIZE + 1, 0))
                } else {
                    currentLogs
                }
            }

            _logs.value += entry
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}