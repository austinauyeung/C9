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
import com.austinauyeung.nyuma.c9.common.domain.AutoHideDetection
import com.austinauyeung.nyuma.c9.common.domain.OrientationHandler
import com.austinauyeung.nyuma.c9.core.logs.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    private val keysPressed: MutableSet<Int> = mutableSetOf()
    private var isTextField: Boolean = false

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
                    backgroundScope.launch {
                        serviceManager.activateGridMode()
                    }
                }
                ACTION_RESET_GRID -> {
                    backgroundScope.launch {
                        serviceManager.resetGrid()
                    }
                }
                ACTION_ACTIVATE_CURSOR -> {
                    backgroundScope.launch {
                        serviceManager.activateCursorMode()
                    }
                }
                ACTION_TOGGLE_CURSOR -> {
                    backgroundScope.launch {
                        serviceManager.toggleCursorScroll()
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
        const val ACTION_RESET_GRID = "com.austinauyeung.nyuma.c9.ACTION_RESET_GRID"
        const val ACTION_ACTIVATE_CURSOR = "com.austinauyeung.nyuma.c9.ACTION_ACTIVATE_CURSOR"
        const val ACTION_TOGGLE_CURSOR = "com.austinauyeung.nyuma.c9.ACTION_TOGGLE_CURSOR"

        fun activateGridCursor(context: Context) {
            val intent = Intent(ACTION_ACTIVATE_GRID)
            intent.setPackage(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.sendBroadcast(intent, null)
            } else {
                context.sendBroadcast(intent)
            }
        }

        fun resetGrid(context: Context) {
            val intent = Intent(ACTION_RESET_GRID)
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

        fun toggleCursorScroll(context: Context) {
            val intent = Intent(ACTION_TOGGLE_CURSOR)
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

            val settingsFlow = C9.getInstance().getSettingsFlow()

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            orientationHandler = OrientationHandler(context = this, settingsFlow = settingsFlow)

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
                addAction(ACTION_RESET_GRID)
                addAction(ACTION_ACTIVATE_CURSOR)
                addAction(ACTION_TOGGLE_CURSOR)
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

    private fun attemptCursorRestore(): Boolean {
        val settings = C9.getInstance().getSettingsFlow().value

        if (settings.hideOnTextField == AutoHideDetection.RESTORE_ON_FOCUS_LOST) {
            if (keysPressed.isNotEmpty() || isTextField) return false
        }

        if (hidingCursor && settings.hideOnTextField != AutoHideDetection.NONE) {
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

            return true
        }

        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val settings = C9.getInstance().getSettingsFlow().value
        if (settings.hideOnTextField != AutoHideDetection.NONE) {
            event?.let{
                when (event.eventType) {
                    // For any detection method, hide keyboard when it opens
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                        val isKeyboardActivated = event.className?.toString() == "android.inputmethodservice.SoftInputWindow"
                        if (isKeyboardActivated && !hidingCursor) autoHideCursor()
                    }

                    AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                        if (settings.hideOnTextField == AutoHideDetection.RESTORE_ON_FOCUS_LOST) {
                            isTextField = (event.source?.className?.contains("EditText") == true) || (event.source?.isEditable == true)
                            if (hidingCursor && !isTextField) attemptCursorRestore()
                            if (!hidingCursor && isTextField) autoHideCursor()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    // Required by AccessibilityService interface
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        val settings = C9.getInstance().getSettingsFlow().value
        if (hidingCursor) {
            if (settings.hideOnTextField == AutoHideDetection.RESTORE_ON_ENTER &&
                event?.keyCode in submissionKeys &&
                event?.action == KeyEvent.ACTION_UP) {
                Logger.d("Enter key pressed, assuming form submission")
                if (attemptCursorRestore()) return false
            }

            if (settings.hideOnTextField == AutoHideDetection.RESTORE_ON_FOCUS_LOST) {
                if (event?.action == KeyEvent.ACTION_DOWN) {
                    keysPressed.add(event.keyCode)
                } else if (event?.action == KeyEvent.ACTION_UP) {
                    keysPressed.remove(event.keyCode)
                    if (keysPressed.isEmpty()) {
                        if (attemptCursorRestore()) return false
                    }
                }
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