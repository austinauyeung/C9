package com.austinauyeung.nyuma.c9.core.logs.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.austinauyeung.nyuma.c9.core.logs.LogManager
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.core.util.SystemInfoUtil

/**
 * Basic console for real-time logs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    viewModel: LogViewModel,
    onNavigateBack: () -> Unit,
) {
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF202020))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) {
                items(logs) { logEntry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatLogEntry(logEntry),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Button(
                onClick = { copyLogsToClipboard(context, logs) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF505050),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Copy")
                }
            }
        }
    }
}

private fun formatLogEntry(logEntry: LogManager.LogEntry): AnnotatedString {
    return buildAnnotatedString {
        pushStyle(SpanStyle(color = Color.Gray))
        append("[${logEntry.formattedTimestamp()}]")
        append(" ")
        pop()

        val (levelChar, levelColor) = when (logEntry.level) {
            Logger.Level.VERBOSE -> "V" to Color.Gray
            Logger.Level.DEBUG -> "D" to Color.Cyan
            Logger.Level.INFO -> "I" to Color.Green
            Logger.Level.WARNING -> "W" to Color.Yellow
            Logger.Level.ERROR -> "E" to Color.Red
        }
        pushStyle(SpanStyle(color = levelColor, fontWeight = FontWeight.Bold))
        append("[$levelChar]")
        pop()

        append(" ")
        pushStyle(SpanStyle(color = Color.White))
        append(logEntry.message)
        pop()
    }
}

private fun copyLogsToClipboard(context: Context, logs: List<LogManager.LogEntry>) {
    val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)

    val formattedLogs = buildString {
        append("--- SYSTEM INFORMATION ---\n")
        append(SystemInfoUtil.getDeviceInfo(context))
        append("\n\n")

        append("--- LOG ENTRIES ---\n")
        logs.forEach { log ->
            append("[${log.formattedTimestamp()}]")
            append(" ")
            append("[${log.level.name.first()}]")
            append(" ")
            append(log.message)
            append("\n")
        }
    }

    val clip = ClipData.newPlainText("Logs with system info", formattedLogs)
    clipboard?.setPrimaryClip(clip)

    Toast.makeText(context, "Logs copied", Toast.LENGTH_SHORT).show()
}