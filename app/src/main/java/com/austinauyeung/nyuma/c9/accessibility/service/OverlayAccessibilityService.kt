package com.austinauyeung.nyuma.c9.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.austinauyeung.nyuma.c9.C9
import com.austinauyeung.nyuma.c9.accessibility.coordinator.AccessibilityServiceManager
import com.austinauyeung.nyuma.c9.accessibility.ui.OverlayUIManager
import com.austinauyeung.nyuma.c9.common.domain.ScreenDimensions
import com.austinauyeung.nyuma.c9.core.util.Logger
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Receives key events, displays overlays, and performs gestures.
 */
class OverlayAccessibilityService : AccessibilityService(), LifecycleOwner,
    SavedStateRegistryOwner {
    private var windowManager: WindowManager? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var serviceJob: Job
    private lateinit var backgroundScope: CoroutineScope
    private lateinit var mainScope: CoroutineScope
    private lateinit var ioScope: CoroutineScope

    private val coroutineExceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            Logger.e("Coroutine error in service", exception)
        }

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var serviceManager: AccessibilityServiceManager
    private lateinit var uiManager: OverlayUIManager

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_ACTIVATE_GRID -> {
                    ioScope.launch {
                        val settings = C9.getInstance().settingsRepository.getSettings().first()
                        if (settings.gridActivationKey == OverlaySettings.KEY_NONE) return@launch
                        serviceManager.activateGridMode()
                    }
                }
                ACTION_ACTIVATE_CURSOR -> {
                    ioScope.launch {
                        val settings = C9.getInstance().settingsRepository.getSettings().first()
                        if (settings.cursorActivationKey == OverlaySettings.KEY_NONE) return@launch
                        serviceManager.activateCursorMode()
                    }
                }
            }
        }
    }

    companion object {
        private var instance: OverlayAccessibilityService? = null

        fun getInstance(): OverlayAccessibilityService? {
            return instance
        }

        fun showToast(message: String) {
            instance?.showToastInternal(message)
        }

        const val ACTION_ACTIVATE_GRID = "com.austinauyeung.nyuma.c9.ACTION_ACTIVATE_GRID"
        const val ACTION_ACTIVATE_CURSOR = "com.austinauyeung.nyuma.c9.ACTION_ACTIVATE_CURSOR"

        fun activateGridCursor(context: Context) {
            val intent = Intent(ACTION_ACTIVATE_GRID)
            intent.setPackage(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.sendBroadcast(intent, null)
            } else {
                context.sendBroadcast(intent)
            }
        }

        fun activateStandardCursor(context: Context) {
            val intent = Intent(ACTION_ACTIVATE_CURSOR)
            intent.setPackage(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.sendBroadcast(intent, null)
            } else {
                context.sendBroadcast(intent)
            }
        }
    }

    private fun showToastInternal(message: String) {
        mainHandler.post {
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        try {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

            serviceJob = SupervisorJob()
            backgroundScope = CoroutineScope(Dispatchers.Default + serviceJob + coroutineExceptionHandler)
            mainScope = CoroutineScope(Dispatchers.Main + serviceJob + coroutineExceptionHandler)
            ioScope = CoroutineScope(Dispatchers.IO + serviceJob + coroutineExceptionHandler)

            val settingsFlow = C9.getInstance().settingsRepository.getSettings().stateIn(
                scope = ioScope,
                started = SharingStarted.Eagerly,
                initialValue = OverlaySettings()
            )

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            val displayMetrics = resources.displayMetrics
            val screenDimensions = ScreenDimensions(
                width = displayMetrics.widthPixels,
                height = displayMetrics.heightPixels
            )

            serviceManager = AccessibilityServiceManager(
                service = this,
                settingsFlow = settingsFlow,
                screenDimensions = screenDimensions,
                backgroundScope = backgroundScope,
                mainScope = mainScope,
            )
            serviceManager.initialize()

            uiManager = OverlayUIManager(
                context = this,
                backgroundScope = backgroundScope,
                mainScope = mainScope,
                windowManager = windowManager!!,
                settingsFlow = settingsFlow,
                accessibilityManager = serviceManager,
                lifecycleOwner = this,
                savedStateRegistryOwner = this
            )
            uiManager.initialize()

            val filter = IntentFilter().apply {
                addAction(ACTION_ACTIVATE_GRID)
                addAction(ACTION_ACTIVATE_CURSOR)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(receiver, filter)
            }

            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            Logger.i("Overlay accessibility service connected")
        } catch (e: Exception) {
            Logger.e("Error initializing service", e)
            if (!::serviceJob.isInitialized) {
                serviceJob = SupervisorJob()
            }
        }
    }

    // Required by AccessibilityService interface
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        return try {
            serviceManager.handleKeyEvent(event)
        } catch (e: Exception) {
            Logger.e("Error processing key event", e)
            false
        }
    }

    fun forceHideAllOverlays() {
        serviceManager.forceHideAllOverlays()
        uiManager.updateOverlayUI()
    }

    override fun onDestroy() {
        instance = null
        try {
            if (::backgroundScope.isInitialized) {
                backgroundScope.cancel("Service destroyed")
            }

            if (::mainScope.isInitialized) {
                mainScope.cancel("Service destroyed")
            }

            if (::ioScope.isInitialized) {
                ioScope.cancel("Service destroyed")
            }

            if (::serviceManager.isInitialized) {
                serviceManager.cleanup()
            }

            if (::uiManager.isInitialized) {
                uiManager.cleanup()
            }

            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
                Logger.e("Error unregistering receiver", e)
            }

            mainHandler.removeCallbacksAndMessages(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

            Logger.i("Overlay accessibility service destroyed")
        } catch (e: Exception) {
            Logger.e("Error during service cleanup", e)
        } finally {
            super.onDestroy()
        }
    }
}