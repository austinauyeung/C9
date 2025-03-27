package com.austinauyeung.nyuma.c9.core.logs.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.austinauyeung.nyuma.c9.common.ui.C9Theme
import com.austinauyeung.nyuma.c9.core.logs.LogManager

/**
 * Basic console for real-time logs.
 */
class LogActivity : ComponentActivity() {
    private lateinit var viewModel: LogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            LogManager.clear()
        }

        val factory = LogViewModel.Factory()
        viewModel = ViewModelProvider(this, factory)[LogViewModel::class.java]

        setContent {
            C9Theme {
                LogScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startLogCollection()
    }

    override fun onStop() {
        viewModel.stopLogCollection()
        super.onStop()
    }
}