package com.austinauyeung.nyuma.c9.accessibility.coordinator

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import com.austinauyeung.nyuma.c9.C9
import com.austinauyeung.nyuma.c9.common.domain.OrientationHandler
import com.austinauyeung.nyuma.c9.common.domain.ScreenDimensions
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.cursor.domain.CursorState
import com.austinauyeung.nyuma.c9.cursor.handler.CursorActionHandler
import com.austinauyeung.nyuma.c9.cursor.handler.CursorStateManager
import com.austinauyeung.nyuma.c9.gesture.api.GestureManager
import com.austinauyeung.nyuma.c9.gesture.shizuku.ShizukuGestureStrategy
import com.austinauyeung.nyuma.c9.gesture.standard.StandardGestureStrategy
import com.austinauyeung.nyuma.c9.gesture.ui.GesturePath
import com.austinauyeung.nyuma.c9.grid.domain.Grid
import com.austinauyeung.nyuma.c9.grid.domain.GridNavigator
import com.austinauyeung.nyuma.c9.grid.handler.GridActionHandler
import com.austinauyeung.nyuma.c9.grid.handler.GridStateManager
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Manages grid cursor and standard cursor modes.
 */
class AccessibilityServiceManager(
    private val service: AccessibilityService,
    private val settingsFlow: StateFlow<OverlaySettings>,
    private val orientationHandler: OrientationHandler,
    private val backgroundScope: CoroutineScope,
    private val mainScope: CoroutineScope
) {
    private lateinit var gestureManager: GestureManager
    private lateinit var cursorStateManager: CursorStateManager
    private lateinit var cursorActionHandler: CursorActionHandler
    private lateinit var gridNavigator: GridNavigator
    private lateinit var gridStateManager: GridStateManager
    private lateinit var gridActionHandler: GridActionHandler
    private lateinit var modeCoordinator: OverlayModeCoordinator

    private val _currentGrid = MutableStateFlow<Grid?>(null)
    val currentGrid: StateFlow<Grid?> = _currentGrid.asStateFlow()

    private val _currentCursor = MutableStateFlow<CursorState?>(null)
    val currentCursor: StateFlow<CursorState?> = _currentCursor.asStateFlow()

    private val screenDimensionsFlow = orientationHandler.screenDimensions

    fun initialize() {
        try {
            Logger.i("Initializing AccessibilityServiceManager")

            modeCoordinator = OverlayModeCoordinator()

            val standardStrategy = StandardGestureStrategy(service, settingsFlow)
            val shizukuStrategy = ShizukuGestureStrategy(
                mainScope = mainScope,
                settingsFlow = settingsFlow
            )
            C9.getInstance().setShizukuGestureStrategy(shizukuStrategy)

            gestureManager = GestureManager(
                standardStrategy,
                shizukuStrategy,
                settingsFlow,
                screenDimensionsFlow,
                backgroundScope
            )

            // Grid components
            gridNavigator = GridNavigator(screenDimensionsFlow)
            gridStateManager = GridStateManager(
                gridNavigator,
                gestureManager,
                settingsFlow,
                screenDimensionsFlow,
                backgroundScope,
                { grid -> onGridStateChanged(grid) }
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
                screenDimensionsFlow,
                { cursorState -> onCursorStateChanged(cursorState) }
            )
            cursorActionHandler = CursorActionHandler(
                cursorStateManager,
                gestureManager,
                settingsFlow,
                backgroundScope,
                modeCoordinator,
                { orientationHandler.getCurrentOrientation() }
            )

            // Listen for orientation changes
            orientationHandler.screenDimensions
                .onEach { newDimensions ->
                    onScreenDimensionsChanged(newDimensions)
                }
                .launchIn(backgroundScope)

            Logger.i("AccessibilityServiceManager initialization complete")
        } catch (e: Exception) {
            Logger.e("Error initializing AccessibilityServiceManager", e)
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

    fun activateGridMode(): Boolean {
        try {
            if (modeCoordinator.requestActivation(OverlayModeCoordinator.OverlayMode.GRID)) {
                gridStateManager.toggleGridVisibility()
                return gridStateManager.isGridVisible()
            }
            return false
        } catch (e: Exception) {
            Logger.e("Error activating grid mode", e)
            return false
        }
    }

    fun activateCursorMode(): Boolean {
        try {
            if (modeCoordinator.requestActivation(OverlayModeCoordinator.OverlayMode.CURSOR)) {
                cursorStateManager.toggleCursorVisibility()
                return cursorStateManager.isCursorVisible()
            }
            return false
        } catch (e: Exception) {
            Logger.e("Error activating cursor mode", e)
            return false
        }
    }

    fun handleKeyEvent(event: KeyEvent?): Boolean {
        Logger.d("Key event: $event")
        val settings = settingsFlow.value

        try {
            // Check grid mode first
            val gridHandled = gridActionHandler.handleKeyEvent(event)
            val cursorHandled = if (!gridHandled) cursorActionHandler.handleKeyEvent(event) else false
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

    private fun onGridStateChanged(grid: Grid?) {
        _currentGrid.value = grid
    }

    private fun onCursorStateChanged(cursorState: CursorState?) {
        _currentCursor.value = cursorState
    }

    // Invoked when setting activation key
    fun forceHideAllOverlays() {
        Logger.d("Force hiding all overlays")

        try {
            if (gridStateManager.isGridVisible()) {
                gridStateManager.hideGrid()
            }

            if (cursorStateManager.isCursorVisible()) {
                cursorStateManager.hideCursor()
            }

            modeCoordinator.deactivate(OverlayModeCoordinator.OverlayMode.GRID)
            modeCoordinator.deactivate(OverlayModeCoordinator.OverlayMode.CURSOR)

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