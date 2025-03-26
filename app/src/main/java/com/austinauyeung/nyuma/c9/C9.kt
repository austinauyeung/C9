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
import com.austinauyeung.nyuma.c9.core.util.VersionUtil
import com.austinauyeung.nyuma.c9.gesture.shizuku.ShizukuGestureStrategy
import com.austinauyeung.nyuma.c9.settings.repository.SettingsRepository
import com.austinauyeung.nyuma.c9.settings.repository.SettingsRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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
    private var shizukuObserverJob: kotlinx.coroutines.Job? = null
    private var _shizukuGestureStrategy: ShizukuGestureStrategy? = null

    fun setShizukuGestureStrategy(strategy: ShizukuGestureStrategy) {
        _shizukuGestureStrategy = strategy
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (VersionUtil.isAndroid11()) {
            initializeShizuku()
        }

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
                        applicationScope.launch {
                            Logger.d("Auto-requesting Shizuku permission")
                            ShizukuServiceConnection.requestPermission()
                        }
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

    override fun onTerminate() {
        Logger.i("C9 application terminating")

        if (VersionUtil.isAndroid11()) {
            shizukuObserverJob?.cancel()
            _shizukuGestureStrategy?.shutdown()
            ShizukuServiceConnection.cleanup()
        }

        applicationScope.cancel()
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
