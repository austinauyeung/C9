package com.austinauyeung.nyuma.c9.core.service

import android.content.pm.PackageManager
import com.austinauyeung.nyuma.c9.core.logs.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import rikka.shizuku.Shizuku
import rikka.shizuku.SystemServiceHelper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages connection to the Shizuku service for privileged operations.
 */
object ShizukuServiceConnection {
    private val initialized = AtomicBoolean(false)

    private val _statusFlow = MutableStateFlow(ShizukuStatus.UNKNOWN)
    val statusFlow: StateFlow<ShizukuStatus> = _statusFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val bindListener =
        Shizuku.OnBinderReceivedListener {
            Logger.d("Shizuku binder received")
            checkPermissionAndUpdateStatus()
        }

    private val deathListener =
        Shizuku.OnBinderDeadListener {
            Logger.d("Shizuku binder dead")
            _statusFlow.value = ShizukuStatus.NOT_AVAILABLE
        }

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            val isGranted = grantResult == PackageManager.PERMISSION_GRANTED
            Logger.d("Shizuku permission result: requestCode=$requestCode, grantResult=$grantResult, isGranted=$isGranted")

            if (isGranted) {
                Logger.d("Shizuku permission granted, verifying")

                if (verifyShizukuPermission()) {
                    Logger.d("Shizuku permission verification successful after grant")
                    _statusFlow.value = ShizukuStatus.READY
                } else {
                    Logger.d("Shizuku permission verification failed after grant")
                    _statusFlow.value = ShizukuStatus.PERMISSION_REQUIRED
                }
            } else {
                Logger.d("Shizuku permission denied")
                _statusFlow.value = ShizukuStatus.PERMISSION_REQUIRED
            }
        }

    private var lastPermissionRequestTime = 0L
    private var permissionRetryCount = 0
    private val maxRetries = 3

    fun initialize(): Boolean {
        if (!initialized.compareAndSet(false, true)) {
            Logger.d("Shizuku already initialized, skipping")
            return false
        }

        try {
            Logger.d("Initializing Shizuku connection")

            Shizuku.addBinderReceivedListener(bindListener)
            Shizuku.addBinderDeadListener(deathListener)
            Shizuku.addRequestPermissionResultListener(permissionListener)

            if (Shizuku.pingBinder()) {
                Logger.d("Shizuku is running, checking permission")

                checkPermissionAndUpdateStatus { status ->
                    if (status == ShizukuStatus.PERMISSION_REQUIRED) {
                        Logger.d("Shizuku permission not granted, requesting")
                        requestPermission()
                    }
                }
            } else {
                Logger.d("Shizuku is not available")
                _statusFlow.value = ShizukuStatus.NOT_AVAILABLE
            }

            return true
        } catch (e: Exception) {
            Logger.e("Failed to initialize Shizuku", e)
            _statusFlow.value = ShizukuStatus.ERROR
            return false
        }
    }

    fun cleanup() {
        if (!initialized.get()) {
            return
        }

        try {
            Logger.d("Cleaning up Shizuku connection")

            Shizuku.removeBinderReceivedListener(bindListener)
            Shizuku.removeBinderDeadListener(deathListener)
            Shizuku.removeRequestPermissionResultListener(permissionListener)

            initialized.set(false)
        } catch (e: Exception) {
            Logger.e("Error during Shizuku cleanup", e)
        }
    }

    fun requestPermission(
        requestCode: Int = 100,
        forceRequest: Boolean = false,
    ) {
        try {
            if (!Shizuku.pingBinder()) {
                Logger.d("Cannot request permission: Shizuku binder not available")
                _statusFlow.value = ShizukuStatus.NOT_AVAILABLE
                return
            }

            val currentTime = System.currentTimeMillis()
            val timeSinceLastRequest = currentTime - lastPermissionRequestTime

            if (!forceRequest && permissionRetryCount > 0) {
                // Exponential backoff
                val backoffTime = (1000L * (1 shl (permissionRetryCount - 1))).coerceAtMost(30000L)

                if (timeSinceLastRequest < backoffTime) {
                    Logger.d(
                        "Skipping permission request due to backoff: " +
                                "retry $permissionRetryCount, waited ${timeSinceLastRequest}ms of ${backoffTime}ms",
                    )
                    return
                }

                if (permissionRetryCount >= maxRetries) {
                    Logger.d("Maximum auto-retry count reached ($maxRetries), waiting for manual request")
                    return
                }
            }

            val shouldShowRationale = Shizuku.shouldShowRequestPermissionRationale()
            Logger.d("Should show permission rationale before request: $shouldShowRationale")

            _statusFlow.value = ShizukuStatus.PERMISSION_REQUIRED

            Logger.d("Requesting Shizuku permission with code: $requestCode (retry: $permissionRetryCount)")
            Shizuku.requestPermission(requestCode)

            lastPermissionRequestTime = currentTime
            permissionRetryCount++
        } catch (e: Exception) {
            Logger.e("Failed to request Shizuku permission", e)
            _statusFlow.value = ShizukuStatus.ERROR
        }
    }

    fun resetPermissionRetryCount() {
        permissionRetryCount = 0
    }

    private fun checkPermissionAndUpdateStatus(onStatusChecked: ((ShizukuStatus) -> Unit)? = null): ShizukuStatus {
        try {
            val isBound = Shizuku.pingBinder()
            Logger.d("Shizuku binder is active: $isBound")

            if (!isBound) {
                val newStatus = ShizukuStatus.NOT_AVAILABLE
                _statusFlow.value = newStatus
                onStatusChecked?.invoke(newStatus)
                return newStatus
            }

            val hasPermission = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            val newStatus =
                if (hasPermission) ShizukuStatus.READY else ShizukuStatus.PERMISSION_REQUIRED

            Logger.d("Shizuku permission status: $newStatus")
            _statusFlow.value = newStatus

            onStatusChecked?.invoke(newStatus)
            return newStatus
        } catch (e: Exception) {
            Logger.e("Failed to check Shizuku status", e)
            val errorStatus = ShizukuStatus.ERROR
            _statusFlow.value = errorStatus
            onStatusChecked?.invoke(errorStatus)
            return errorStatus
        }
    }

    // Verify Shizuku with API call
    private fun verifyShizukuPermission(): Boolean {
        try {
            val binder = SystemServiceHelper.getSystemService("input")
            if (binder == null) {
                Logger.d("Failed to get 'input' service binder")
                return false
            }

            return true
        } catch (e: Exception) {
            Logger.e("Shizuku permission verification failed", e)
            return false
        }
    }

    fun isReady(forceRefresh: Boolean = false): Boolean {
        if (forceRefresh) {
            val currentStatus = checkPermissionAndUpdateStatus()
            return currentStatus == ShizukuStatus.READY
        }
        return _statusFlow.value == ShizukuStatus.READY
    }

    fun refreshStatus(): ShizukuStatus {
        Logger.d("Manually refreshing Shizuku status")
        return checkPermissionAndUpdateStatus()
    }

    fun observeStatus(observer: (ShizukuStatus) -> Unit): Job {
        return statusFlow.onEach { status ->
            observer(status)
        }.launchIn(scope)
    }
}
