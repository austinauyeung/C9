package com.austinauyeung.nyuma.c9.core.control

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import com.austinauyeung.nyuma.c9.C9
import com.austinauyeung.nyuma.c9.core.domain.OrientationHandler
import com.austinauyeung.nyuma.c9.core.domain.ScreenDimensions
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.core.notification.NotificationManager
import com.austinauyeung.nyuma.c9.cursor.control.CursorActionHandler
import com.austinauyeung.nyuma.c9.cursor.control.CursorStateManager
import com.austinauyeung.nyuma.c9.gesture.api.GestureManager
import com.austinauyeung.nyuma.c9.gesture.shizuku.ShizukuGestureStrategy
import com.austinauyeung.nyuma.c9.gesture.standard.DefaultGestureStrategy
import com.austinauyeung.nyuma.c9.gesture.ui.GesturePath
import com.austinauyeung.nyuma.c9.grid.control.GridActionHandler
import com.austinauyeung.nyuma.c9.grid.control.GridStateManager
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Manages grid cursor and standard cursor modes.
 */
class CoreManager(
    private val service: AccessibilityService,
    private val settingsFlow: StateFlow<OverlaySettings>,
    private val orientationHandler: OrientationHandler,
    private val backgroundScope: CoroutineScope,
    private val keysPressed: MutableStateFlow<Int>,
    private val layoutApplied: SharedFlow<Unit>
) {
    private lateinit var gestureManager: GestureManager
    lateinit var cursorStateManager: CursorStateManager
    private lateinit var cursorActionHandler: CursorActionHandler
    lateinit var gridStateManager: GridStateManager
    private lateinit var gridActionHandler: GridActionHandler
    lateinit var modeCoordinator: ModeCoordinator
    private lateinit var notificationManager: NotificationManager

    private val screenDimensionsFlow = orientationHandler.screenDimensions

    data class ChannelMessage(
        val event: KeyEvent,
        val handler: suspend (KeyEvent) -> Unit
    )

    private val channel = Channel<ChannelMessage>(Channel.UNLIMITED)
    init {
        backgroundScope.launch(Dispatchers.Default) {
            for (msg in channel) {
                processMessage(msg)
            }
        }
    }
    private suspend fun processMessage(msg: ChannelMessage) = coroutineScope {
        val settings = settingsFlow.value

        if (msg.event.action == KeyEvent.ACTION_DOWN) {
            keysPressed.value += 1
            if (keysPressed.value == 1 && settings.disableTouchscreen) {
                layoutApplied.first()
            }
        }

        msg.handler(msg.event)

        if (msg.event.action == KeyEvent.ACTION_UP) {
            keysPressed.value -= 1
            if (keysPressed.value == 0 && settings.disableTouchscreen) {
                layoutApplied.first()
            }
        }
    }

    fun initialize() {
        try {
            Logger.i("Initializing CoreManager")

            modeCoordinator = ModeCoordinator()
            notificationManager = NotificationManager(service)

            val defaultStrategy = DefaultGestureStrategy(service, settingsFlow)
            val shizukuStrategy = ShizukuGestureStrategy(
                settingsFlow = settingsFlow
            )
            C9.getInstance().setShizukuGestureStrategy(shizukuStrategy)

            gestureManager = GestureManager(
                defaultStrategy,
                shizukuStrategy,
                settingsFlow,
                screenDimensionsFlow,
                backgroundScope
            )

            // Grid components
            gridStateManager = GridStateManager(
                gestureManager,
                settingsFlow,
                screenDimensionsFlow
            )
            gridActionHandler = GridActionHandler(
                gridStateManager,
                gestureManager,
                settingsFlow,
                backgroundScope,
                modeCoordinator,
                { orientationHandler.getCurrentOrientation() }
            )

            // Cursor components
            cursorStateManager = CursorStateManager(
                settingsFlow,
                screenDimensionsFlow
            )
            cursorActionHandler = CursorActionHandler(
                cursorStateManager,
                gestureManager,
                settingsFlow,
                backgroundScope,
                modeCoordinator,
                { orientationHandler.getCurrentOrientation() },
                screenDimensionsFlow
            )

            // Listen for orientation changes
            orientationHandler.screenDimensions
                .onEach { newDimensions ->
                    onScreenDimensionsChanged(newDimensions)
                }
                .launchIn(backgroundScope)

            // Listen for mode changes to update notification
            modeCoordinator.activeMode
                .onEach { mode ->
                    updateNotification(mode)
                }
                .launchIn(backgroundScope)

            settingsFlow
                .onEach {
                    updateNotification(modeCoordinator.activeMode.value)
                }
                .launchIn(backgroundScope)

            Logger.i("CoreManager initialization complete")
        } catch (e: Exception) {
            Logger.e("Error initializing CoreManager", e)
            throw e
        }
    }

    private fun onScreenDimensionsChanged(newDimensions: ScreenDimensions) {
        try {
            if (gridStateManager.isGridVisible()) {
                gridStateManager.resetToMainGrid(force = true)
            }

            if (cursorStateManager.isCursorVisible()) {
                val (centerX, centerY) = newDimensions.center()
                cursorStateManager.updatePosition(Offset(centerX, centerY))
            }
        } catch (e: Exception) {
            Logger.e("Error handling screen dimensions change", e)
        }
    }

    fun activateGridMode(keymapToggle: Boolean = false): Boolean {
        try {
            if ((!gridStateManager.isGridVisible() || keymapToggle) && modeCoordinator.requestActivation(
                    ModeCoordinator.OverlayMode.GRID
                )) {
                gridStateManager.toggleGridVisibility()
                return gridStateManager.isGridVisible()
            }
            return false
        } catch (e: Exception) {
            Logger.e("Error activating grid mode", e)
            return false
        }
    }

    fun resetGrid(): Boolean {
        try {
            if (modeCoordinator.activeMode.value == ModeCoordinator.OverlayMode.GRID) {
                gridStateManager.resetToMainGrid()
                return true
            }
            return false
        } catch (e: Exception) {
            Logger.e("Error resetting grid mode", e)
            return false
        }
    }

    fun activateCursorMode(keymapToggle: Boolean = false): Boolean {
        try {
            if ((!cursorStateManager.isCursorVisible() || keymapToggle) && modeCoordinator.requestActivation(
                    ModeCoordinator.OverlayMode.CURSOR
                )) {
                cursorStateManager.toggleCursorVisibility()
                return cursorStateManager.isCursorVisible()
            }
            return false
        } catch (e: Exception) {
            Logger.e("Error activating cursor mode", e)
            return false
        }
    }

    fun toggleCursorScroll(): Boolean {
        try {
            if (modeCoordinator.activeMode.value == ModeCoordinator.OverlayMode.CURSOR) {
                cursorStateManager.toggleScrollMode()
                return true
            }
            return false
        } catch (e: Exception) {
            Logger.e("Error toggling cursor scroll", e)
            return false
        }
    }

    fun handleKeyEvent(event: KeyEvent?): Boolean {
        Logger.d("Key event: $event")
        val settings = settingsFlow.value

        try {
            // Check grid mode first
            val gridHandled = gridActionHandler.handleKeyEvent(event, channel)
            val cursorHandled = if (!gridHandled) cursorActionHandler.handleKeyEvent(event, channel) else false
            val eventHandled = gridHandled || cursorHandled

            if (settings.allowPassthrough) {
                Logger.d("Allowing key event to pass through")
            }

            return !settings.allowPassthrough && eventHandled
        } catch (e: Exception) {
            Logger.e("Error processing key event", e)
            return false
        }
    }

    private fun updateNotification(mode: ModeCoordinator.OverlayMode) {
        val settings = settingsFlow.value

        try {
            if (settings.showNotification) {
                when (mode) {
                    ModeCoordinator.OverlayMode.OFF -> {
                        notificationManager.hideNotification()
                    }

                    else -> {
                        notificationManager.showNotification(mode)
                    }
                }
            } else {
                notificationManager.hideNotification()
            }
        } catch (e: Exception) {
            Logger.e("Error updating notification", e)
        }
    }

    // Invoked when setting activation key
    fun forceHideAllOverlays(fromAutoHide: Boolean) {
        Logger.d("Force hiding all overlays")

        try {
            if (gridStateManager.isGridVisible()) {
                gridStateManager.hideGrid()
            }

            if (cursorStateManager.isCursorVisible()) {
                cursorStateManager.hideCursor()
            }

            modeCoordinator.deactivate(ModeCoordinator.OverlayMode.GRID, fromAutoHide)
            modeCoordinator.deactivate(ModeCoordinator.OverlayMode.CURSOR, fromAutoHide)

            gridActionHandler.cleanup()
            cursorActionHandler.cleanup()
        } catch (e: Exception) {
            Logger.e("Error force hiding overlays", e)
        }
    }

    fun updateGestureVisualization(showGestures: Boolean) {
        gestureManager.updateGestureVisibility(showGestures)
    }

    fun getGesturePaths(): StateFlow<List<GesturePath>> {
        return gestureManager.gesturePaths
    }

    fun cleanup() {
        gridActionHandler.cleanup()
        cursorActionHandler.cleanup()
        gestureManager.cleanup()
    }
}