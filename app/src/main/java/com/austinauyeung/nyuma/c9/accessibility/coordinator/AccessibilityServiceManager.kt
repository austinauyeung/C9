package com.austinauyeung.nyuma.c9.accessibility.coordinator

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import com.austinauyeung.nyuma.c9.C9
import com.austinauyeung.nyuma.c9.common.domain.ScreenDimensions
import com.austinauyeung.nyuma.c9.core.util.Logger
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

/**
 * Manages grid cursor and standard cursor modes.
 */
class AccessibilityServiceManager(
    private val service: AccessibilityService,
    private val settingsFlow: StateFlow<OverlaySettings>,
    private val screenDimensions: ScreenDimensions,
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
                screenDimensions,
                backgroundScope
            )

            // Grid components
            gridNavigator = GridNavigator(screenDimensions)
            gridStateManager = GridStateManager(
                gridNavigator,
                gestureManager,
                settingsFlow,
                screenDimensions,
                { grid -> onGridStateChanged(grid) }
            )
            gridActionHandler = GridActionHandler(
                gridStateManager,
                gestureManager,
                settingsFlow,
                backgroundScope,
                modeCoordinator
            )

            // Cursor components
            cursorStateManager = CursorStateManager(
                settingsFlow,
                screenDimensions,
                { cursorState -> onCursorStateChanged(cursorState) }
            )
            cursorActionHandler = CursorActionHandler(
                cursorStateManager,
                gestureManager,
                settingsFlow,
                backgroundScope,
                modeCoordinator
            )

            Logger.i("AccessibilityServiceManager initialization complete")
        } catch (e: Exception) {
            Logger.e("Error initializing AccessibilityServiceManager", e)
            throw e
        }
    }

    fun handleKeyEvent(event: KeyEvent?): Boolean {
        Logger.d("Key event: $event")
        try {
            // Check grid mode first
            if (gridActionHandler.handleKeyEvent(event)) {
                return true
            }

            return cursorActionHandler.handleKeyEvent(event)
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
    }
}