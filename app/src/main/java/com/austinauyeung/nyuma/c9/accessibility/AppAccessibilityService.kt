package com.austinauyeung.nyuma.c9.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Build
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.austinauyeung.nyuma.c9.C9
import com.austinauyeung.nyuma.c9.core.constants.ApplicationConstants
import com.austinauyeung.nyuma.c9.core.control.CoreManager
import com.austinauyeung.nyuma.c9.core.control.ModeCoordinator
import com.austinauyeung.nyuma.c9.core.control.OverlayManager
import com.austinauyeung.nyuma.c9.core.domain.OrientationHandler
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.settings.domain.AppListType
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
class AppAccessibilityService : AccessibilityService(), LifecycleOwner,
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
            Logger.e("Coroutine error in shizuku", exception)
        }

    private lateinit var coreManager: CoreManager
    private lateinit var overlayManager: OverlayManager
    private lateinit var orientationHandler: OrientationHandler

    private var lastOverlayType: ModeCoordinator.OverlayMode = ModeCoordinator.OverlayMode.OFF

    private val keysPressed: MutableSet<Int> = mutableSetOf()

    private var lastKeyboardState = false
    private var lastLockScreenState = false
    private var lastAppState: Boolean? = null
    private var autoHideJob: Job? = null
    private val keyguardManager by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    private val imm by lazy { getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
    private val windowHeightMethod by lazy { InputMethodManager::class.java.getMethod("getInputMethodWindowVisibleHeight")}

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_ACTIVATE_GRID -> {
                    backgroundScope.launch {
                        coreManager.activateGridMode(false)
                    }
                }
                ACTION_RESET_GRID -> {
                    backgroundScope.launch {
                        coreManager.resetGrid()
                    }
                }
                ACTION_ACTIVATE_CURSOR -> {
                    backgroundScope.launch {
                        coreManager.activateCursorMode(false)
                    }
                }
                ACTION_TOGGLE_CURSOR -> {
                    backgroundScope.launch {
                        coreManager.toggleCursorScroll()
                    }
                }
            }
        }
    }

    companion object {
        private var instance: AppAccessibilityService? = null

        fun getInstance(): AppAccessibilityService? {
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

    private fun _isNodeClickable(node: AccessibilityNodeInfo?, pos: Offset?): Boolean {
        if (node == null || pos == null) return false

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val x = pos.x.toInt()
        val y = pos.y.toInt()

        if (!bounds.contains(x, y)) {
            return false
        }

        val isClickable = node.isClickable ||
                node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
        if (isClickable) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (_isNodeClickable(child, pos)) {
                return true
            }
        }

        return false
    }

    fun isNodeClickable(pos: Offset?): Boolean {
        return _isNodeClickable(rootInActiveWindow, pos)
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

            coreManager = CoreManager(
                service = this,
                settingsFlow = settingsFlow,
                orientationHandler = orientationHandler,
                backgroundScope = backgroundScope,
                mainScope = mainScope,
            )
            coreManager.initialize()

            overlayManager = OverlayManager(
                context = this,
                backgroundScope = backgroundScope,
                mainScope = mainScope,
                windowManager = windowManager!!,
                settingsFlow = settingsFlow,
                orientationHandler = orientationHandler,
                coreManager = coreManager,
                lifecycleOwner = this,
                savedStateRegistryOwner = this
            )
            overlayManager.initialize()

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
        val currentOverlay = coreManager.modeCoordinator.activeMode.value
        val cursorOff = currentOverlay == ModeCoordinator.OverlayMode.OFF
        val cursorAlreadyHidden = currentOverlay == ModeCoordinator.OverlayMode.AUTOHIDDEN
        if (cursorOff || cursorAlreadyHidden)
            return

        lastOverlayType = currentOverlay
        if (lastOverlayType == ModeCoordinator.OverlayMode.CURSOR) {
            coreManager.cursorStateManager.cursorState.value?.let { cursor ->
                coreManager.cursorStateManager.setLastCursorPosition(Offset(cursor.position.x, cursor.position.y))
            }
        }

        Logger.d("Hiding cursor overlay")
        forceHideAllOverlays(true)
    }

    private fun attemptCursorRestore() {
        val currentOverlay = coreManager.modeCoordinator.activeMode.value
        if (currentOverlay != ModeCoordinator.OverlayMode.AUTOHIDDEN)
            return

        Logger.d("Restoring cursor overlay")
        val settings = C9.getInstance().getSettingsFlow().value
        val gridMapped = settings.gridActivationKey != ApplicationConstants.OVERLAY_DISABLED
        val gridLost = (lastOverlayType == ModeCoordinator.OverlayMode.GRID) && !gridMapped
        val cursorMapped = settings.cursorActivationKey != ApplicationConstants.OVERLAY_DISABLED
        val cursorLost = (lastOverlayType == ModeCoordinator.OverlayMode.CURSOR) && !cursorMapped

        // Edge case: cursor previously autohidden and then cleared
        if (gridLost || cursorLost) {
            lastOverlayType = ModeCoordinator.OverlayMode.OFF
        }

        // If no previous overlay type, default to any mapped cursor
        if (lastOverlayType == ModeCoordinator.OverlayMode.OFF) {
            lastOverlayType = when {
                cursorMapped -> ModeCoordinator.OverlayMode.CURSOR
                gridMapped -> ModeCoordinator.OverlayMode.GRID
                else -> ModeCoordinator.OverlayMode.OFF
            }
        }

        when (lastOverlayType) {
            ModeCoordinator.OverlayMode.GRID -> {
                coreManager.activateGridMode(toggle = false)
            }

            ModeCoordinator.OverlayMode.CURSOR -> {
                coreManager.activateCursorMode(toggle = false)
            }

            else -> {}
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val settings = C9.getInstance().getSettingsFlow().value

        event.let {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOWS_CHANGED, AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    if (settings.hideOnKeyboardOpen) {
                        checkKeyboardVisibility()
                    }

                    if (settings.hideOnLockScreen) {
                        checkLockScreenVisibility()
                    }

                    checkAppVisibility()
                }

                else -> {}
            }
        }

        onAutoHideConditionChanged(lastKeyboardState || lastLockScreenState || lastAppState!!)
    }

    private fun checkKeyboardVisibility() {
        try {
            lastKeyboardState = isImeWindowPresent() ||
                    windowHeightMethod.invoke(imm) as Int > 0
            Logger.d("Autohide by keyboard: $lastKeyboardState")
        } catch (e: Exception) {
            Logger.e("Error checking keyboard visibility", e)
        }
    }

    private fun isImeWindowPresent(): Boolean {
        for (window in windows) {
            if (window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                return true
            }
        }
        return false
    }

    private fun checkAppVisibility() {
        try {
            lastAppState = shouldHideInCurrentApp()
            Logger.d("Autohide by current app: $lastAppState")
        } catch (e: Exception) {
            Logger.e("Error checking app visibility", e)
        }
    }

    private fun shouldHideInCurrentApp(): Boolean {
        val settings = C9.getInstance().getSettingsFlow().value
        if (settings.autoHideApps.isEmpty()) return settings.applicationListType == AppListType.ALLOW_LIST

        for (window in windows) {
            val pkg = window.root?.packageName?.toString()
            if (pkg != null && pkg in settings.autoHideApps) {
                return settings.applicationListType == AppListType.DENY_LIST
            }
        }
        return settings.applicationListType == AppListType.ALLOW_LIST
    }

    fun showClickableInCurrentApp(): Boolean {
        val settings = C9.getInstance().getSettingsFlow().value
        val showByDefault = settings.clickableListType == AppListType.DENY_LIST

        if (settings.clickableApps.isEmpty()) return showByDefault

        for (window in windows) {
            val pkg = window.root?.packageName?.toString()
            if (pkg != null && pkg in settings.clickableApps) {
                return !showByDefault
            }
        }
        return showByDefault
    }

    private fun checkLockScreenVisibility() {
        try {
            lastLockScreenState = keyguardManager.isKeyguardLocked
            Logger.d("Autohide by lockscreen: $lastLockScreenState")
        } catch (e: Exception) {
            Logger.e("Error checking lock screen visibility", e)
        }
    }

    private fun onAutoHideConditionChanged(shouldHide: Boolean) {
        autoHideJob?.cancel()
        autoHideJob = mainScope.launch {
            delay(100L)
            if (shouldHide) {
                autoHideCursor()
            } else {
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
            coreManager.handleKeyEvent(event)
        } catch (e: Exception) {
            Logger.e("Error processing key event", e)
            false
        }
    }

    fun forceHideAllOverlays(fromAutoHide: Boolean) {
        coreManager.forceHideAllOverlays(fromAutoHide)
        overlayManager.updateOverlayUI()
    }

    override fun onDestroy() {
        instance = null
        try {
            if (::backgroundScope.isInitialized) {
                backgroundScope.cancel("AppAccessibilityService destroyed")
            }

            if (::mainScope.isInitialized) {
                mainScope.cancel("AppAccessibilityService destroyed")
            }

            if (::coreManager.isInitialized) {
                coreManager.cleanup()
            }

            if (::overlayManager.isInitialized) {
                overlayManager.cleanup()
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

            Logger.i("Overlay accessibility shizuku destroyed")
        } catch (e: Exception) {
            Logger.e("Error during shizuku cleanup", e)
        } finally {
            super.onDestroy()
        }
    }
}