package com.austinauyeung.nyuma.c9.core.logs

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object LogManager {
    private const val LOG_MAX_SIZE = 1000
    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _logFlow = MutableSharedFlow<LogEntry>(extraBufferCapacity = 100)
    val logFlow: SharedFlow<LogEntry> = _logFlow.asSharedFlow()

    data class LogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val level: Logger.Level,
        val message: String,
        val tag: String? = null
    ) {
        fun formattedTimestamp(): String = dateFormat.format(Date(timestamp))
    }

    fun addLog(level: Logger.Level, message: String, tag: String? = null) {
        val entry = LogEntry(level = level, message = message, tag = tag)
        logQueue.add(entry)
        _logFlow.tryEmit(entry)

        while (logQueue.size > LOG_MAX_SIZE) {
            logQueue.poll()
        }
    }

    fun getLogs(): List<LogEntry> {
        return logQueue.toList()
    }

    fun clear() {
        logQueue.clear()
    }
}