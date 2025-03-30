package com.austinauyeung.nyuma.c9.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.austinauyeung.nyuma.c9.C9
import com.austinauyeung.nyuma.c9.accessibility.coordinator.AccessibilityServiceManager
import com.austinauyeung.nyuma.c9.accessibility.coordinator.OverlayModeCoordinator
import com.austinauyeung.nyuma.c9.accessibility.ui.OverlayUIManager
import com.austinauyeung.nyuma.c9.common.domain.OrientationHandler
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
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

    private lateinit var serviceManager: AccessibilityServiceManager
    private lateinit var uiManager: OverlayUIManager
    private lateinit var orientationHandler: OrientationHandler

    private var lastCursorPosition: Offset? = null
    private var lastOverlayType: OverlayModeCoordinator.OverlayMode? = null
    private var hidingCursor: Boolean = false

    // Taking some guesses here and may need to revise
    private val submissionKeys = setOf(
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_CALL,
        KeyEvent.KEYCODE_SEARCH
    )

    fun setHidingCursor(hiding: Boolean) {
        hidingCursor = hiding
    }

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

            val settingsFlow = C9.getInstance().getSettingsFlow()

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            orientationHandler = OrientationHandler(this)

            serviceManager = AccessibilityServiceManager(
                service = this,
                settingsFlow = settingsFlow,
                orientationHandler = orientationHandler,
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
                orientationHandler = orientationHandler,
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

    private fun autoHideCursor() {
        if (serviceManager.currentGrid.value != null) {
            lastOverlayType = OverlayModeCoordinator.OverlayMode.GRID
        } else if (serviceManager.currentCursor.value != null) {
            lastOverlayType = OverlayModeCoordinator.OverlayMode.CURSOR
            serviceManager.currentCursor.value?.let { cursor ->
                lastCursorPosition = Offset(cursor.position.x, cursor.position.y)
            }
        }

        Logger.d("Text field focused, hiding cursor overlays")
        forceHideAllOverlays()
        hidingCursor = true
    }

    private fun restoreCursor() {
        when (lastOverlayType) {
            OverlayModeCoordinator.OverlayMode.GRID -> {
                serviceManager.activateGridMode()
            }

            OverlayModeCoordinator.OverlayMode.CURSOR -> {
                serviceManager.activateCursorMode(lastCursorPosition)
            }

            else -> {}
        }
        lastCursorPosition = null
        lastOverlayType = null
        hidingCursor = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val settingsFlow = C9.getInstance().getSettingsFlow()
        if (settingsFlow.value.hideOnTextField) {
            event?.let{
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    val isKeyboardActivated = event.className?.toString() == "android.inputmethodservice.SoftInputWindow"
                    if (isKeyboardActivated && !hidingCursor) autoHideCursor()
                }
            }
        }
    }

    // Required by AccessibilityService interface
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (hidingCursor) {
            if (event?.keyCode in submissionKeys && event?.action == KeyEvent.ACTION_UP) {
                Logger.d("Enter key pressed, assuming form submission")
                restoreCursor()
                return false
            }
        }

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

            if (::orientationHandler.isInitialized) {
                orientationHandler.cleanup()
            }

            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
                Logger.e("Error unregistering receiver", e)
            }

            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

            Logger.i("Overlay accessibility service destroyed")
        } catch (e: Exception) {
            Logger.e("Error during service cleanup", e)
        } finally {
            super.onDestroy()
        }
    }
}