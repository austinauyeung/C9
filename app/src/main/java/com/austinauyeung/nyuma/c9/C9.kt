package com.austinauyeung.nyuma.c9

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.austinauyeung.nyuma.c9.accessibility.service.OverlayAccessibilityService
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.core.service.ShizukuServiceConnection
import com.austinauyeung.nyuma.c9.core.service.ShizukuStatus
import com.austinauyeung.nyuma.c9.gesture.shizuku.ShizukuGestureStrategy
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import com.austinauyeung.nyuma.c9.settings.repository.SettingsRepository
import com.austinauyeung.nyuma.c9.settings.repository.SettingsRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * Checks accessibility service and initializes Shizuku service on Android 11.
 */
class C9 : Application() {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val _settingsRepository: Lazy<SettingsRepository> =
        lazy {
            SettingsRepositoryImpl(applicationContext.dataStore)
        }
    val settingsRepository: SettingsRepository by _settingsRepository
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var shizukuObserverJob: Job? = null
    private var _shizukuGestureStrategy: ShizukuGestureStrategy? = null
    private var settingsObserverJob: Job? = null

    fun getSettingsFlow(): StateFlow<OverlaySettings> {
        return settingsRepository.getSettings().stateIn(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Eagerly,
            initialValue = OverlaySettings()
        )
    }

    fun setShizukuGestureStrategy(strategy: ShizukuGestureStrategy) {
        _shizukuGestureStrategy = strategy
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        settingsObserverJob = getSettingsFlow()
            .onEach { settings ->
                if (settings.enableShizukuIntegration) {
                    if (shizukuObserverJob == null) {
                        initializeShizuku()
                    }
                } else {
                    cleanupShizuku()
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(applicationScope)

        Logger.i("C9 application initialized")
    }

    private fun initializeShizuku() {
        Logger.i("Initializing Shizuku on Android 11")
        ShizukuServiceConnection.initialize()
        shizukuObserverJob =
            ShizukuServiceConnection.observeStatus { status ->
                Logger.d("Shizuku status changed: $status")

                when (status) {
                    ShizukuStatus.PERMISSION_REQUIRED -> {
                        Logger.d("Auto-requesting Shizuku permission")
                        ShizukuServiceConnection.requestPermission()
                    }

                    ShizukuStatus.NOT_AVAILABLE -> {
                        ShizukuServiceConnection.resetPermissionRetryCount()
                        _shizukuGestureStrategy?.reset()
                    }

                    ShizukuStatus.ERROR -> {
                        _shizukuGestureStrategy?.reset()
                    }

                    ShizukuStatus.READY -> {
                        ShizukuServiceConnection.resetPermissionRetryCount()
                        Logger.i("Shizuku ready")
                    }

                    else -> {}
                }
            }
    }

    private fun cleanupShizuku() {
        shizukuObserverJob?.cancel()
        _shizukuGestureStrategy?.shutdown()
        ShizukuServiceConnection.cleanup()
        shizukuObserverJob?.cancel()
    }

    override fun onTerminate() {
        Logger.i("C9 application terminating")

        settingsObserverJob?.cancel()
        applicationScope.cancel()
        cleanupShizuku()

        super.onTerminate()
    }

    companion object {
        private lateinit var instance: C9

        fun getInstance(): C9 {
            return instance
        }

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            try {
                val am =
                    context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                val enabledServices =
                    am.getEnabledAccessibilityServiceList(
                        AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
                    )
                return enabledServices.any {
                    it.id.contains(context.packageName) &&
                            it.id.contains(OverlayAccessibilityService::class.java.simpleName)
                }
            } catch (e: Exception) {
                Logger.e("Error checking accessibility service status", e)
                return false
            }
        }
    }
}
