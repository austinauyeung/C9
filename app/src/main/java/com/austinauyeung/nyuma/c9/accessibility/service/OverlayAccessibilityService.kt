package com.austinauyeung.nyuma.c9.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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

    private var lastOverlayType: OverlayModeCoordinator.OverlayMode? = null
    private var hidingCursor: Boolean = false

    private val keysPressed: MutableSet<Int> = mutableSetOf()

    private var lastKeyboardState = false
    private var lastLauncherState = false
    private var lastLockScreenState = false
    private var autoHideJob: Job? = null
    private val imeKeywords = listOf("inputmethod", "ime", "io.github.sspanak.tt9")
    private val launcherPackages: Set<String> by lazy {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        resolveInfos.mapNotNull { it.activityInfo?.packageName }.toSet()
    }
    private val keyguardManager by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    private val imm by lazy { getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
    private val windowHeightMethod by lazy { InputMethodManager::class.java.getMethod("getInputMethodWindowVisibleHeight")}

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
                serviceManager.cursorStateManager.setLastCursorPosition(Offset(cursor.position.x, cursor.position.y))
            }
        }

        Logger.d("Hiding cursor overlay")
        forceHideAllOverlays()
        hidingCursor = true
    }

    private fun attemptCursorRestore(): Boolean {
        Logger.d("Restoring cursor overlay")

        if (keysPressed.isNotEmpty()) return false

        if (hidingCursor) {
            when (lastOverlayType) {
                OverlayModeCoordinator.OverlayMode.GRID -> {
                    serviceManager.activateGridMode(toggle = false)
                }

                OverlayModeCoordinator.OverlayMode.CURSOR -> {
                    serviceManager.activateCursorMode(toggle = false)
                }

                else -> {}
            }
            lastOverlayType = null
            hidingCursor = false

            return true
        }

        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val settings = C9.getInstance().getSettingsFlow().value
        event?.let {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {}

                else -> {
                    if (settings.hideOnKeyboardOpen) {
                        checkKeyboardVisibility()
                    }
                    if (settings.hideOnLauncherOpen) {
                        checkLauncherVisibility()
                    }
                    if (settings.hideOnLockScreen) {
                        checkLockScreenVisibility()
                    }

                    onAutoHideConditionChanged(lastKeyboardState || lastLauncherState || lastLockScreenState)
                }
            }
        }
    }

    private fun checkKeyboardVisibility() {
        try {
            val isKeyboardVisible = isImeWindowPresent() ||
                    windowHeightMethod.invoke(imm) as Int > 0
            if (isKeyboardVisible != lastKeyboardState) {
                lastKeyboardState = isKeyboardVisible
            }
        } catch (e: Exception) {
            Logger.e("Error checking keyboard visibility", e)
        }
    }

    private fun isImeWindowPresent(): Boolean {
        for (window in windows) {
            val pkg = window.root?.packageName?.toString() ?: continue
            val pkgLower = pkg.lowercase()

            if (imeKeywords.any { it in pkgLower }) {
                return true
            }
        }
        return false
    }

    private fun checkLauncherVisibility() {
        try {
            val isLauncherVisible = isLauncherPresent()
            if (isLauncherVisible != lastLauncherState) {
                lastLauncherState = isLauncherVisible
            }
        } catch (e: Exception) {
            Logger.e("Error checking launcher visibility", e)
        }
    }

    private fun isLauncherPresent(): Boolean {
        for (window in windows) {
            val pkg = window.root?.packageName?.toString() ?: continue
            if (pkg in launcherPackages) {
                return true
            }
        }
        return false
    }

    private fun checkLockScreenVisibility() {
        try {
            val isLockScreenVisible = keyguardManager.isKeyguardLocked
            if (isLockScreenVisible != lastLockScreenState) {
                lastLockScreenState = isLockScreenVisible
            }
        } catch (e: Exception) {
            Logger.e("Error checking launcher visibility", e)
        }
    }

    private fun onAutoHideConditionChanged(visible: Boolean) {
        autoHideJob?.cancel()
        autoHideJob = mainScope.launch {
            delay(100L)
            if (visible && !hidingCursor) {
                Logger.d("Auto-hide condition changed: hiding cursor")
                autoHideCursor()
            } else if (!visible && hidingCursor) {
                Logger.d("Auto-hide condition changed: attempting to restore cursor")
                attemptCursorRestore()
            }
        }
    }

    // Required by AccessibilityService interface
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            keysPressed.add(event.keyCode)
        } else if (event?.action == KeyEvent.ACTION_UP) {
            keysPressed.remove(event.keyCode)
        }
        Logger.d("Current key presses: $keysPressed")

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