package com.austinauyeung.nyuma.c9.settings.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val hasLauncherActivity: Boolean = false,
    val isHomeLauncher: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoHideAppsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showSystemApps by remember { mutableStateOf(false) }

    LaunchedEffect(showSystemApps) {
        isLoading = true
        installedApps = withContext(Dispatchers.IO) {
            loadInstalledApps(context.packageManager, showSystemApps)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Applications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//            ) {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = "Show system apps",
//                        modifier = Modifier.weight(1f),
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//                    Switch(
//                        checked = showSystemApps,
//                        onCheckedChange = { showSystemApps = it }
//                    )
//                }
//
//                Text(
//                    text = "Showing ${installedApps.size} apps",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    modifier = Modifier.padding(top = 4.dp)
//                )
//            }
//
//            HorizontalDivider()

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(installedApps) { app ->
                        AppAutoHideItem(
                            app = app,
                            isSelected = app.packageName in uiState.autoHideApps,
                            onSelectionChanged = { isSelected ->
                                val newSet = if (isSelected) {
                                    uiState.autoHideApps + app.packageName
                                } else {
                                    uiState.autoHideApps - app.packageName
                                }
                                viewModel.updatePreference(newSet) { settings, v ->
                                    settings.copy(autoHideApps = v)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppAutoHideItem(
    app: AppInfo,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var appIcon by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(app.packageName) {
        withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(app.packageName)
                val bitmap = drawable.toBitmap(48, 48)
                appIcon = bitmap.asImageBitmap()
            } catch (_: Exception) {}
        }
    }

    Surface(
        onClick = { onSelectionChanged(!isSelected) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appIcon != null) {
                androidx.compose.foundation.Image(
                    bitmap = appIcon!!,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
//                if (app.isHomeLauncher) {
//                    Text(
//                        text = "Home launcher",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.tertiary
//                    )
//                } else if (app.hasLauncherActivity) {
//                    Text(
//                        text = if (app.isSystemApp) "System app" else "User app",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = if (app.isSystemApp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
//                    )
//                } else {
//                    Text(
//                        text = "System app",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.outline
//                    )
//                }
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChanged
            )
        }
    }
}

suspend fun loadInstalledApps(packageManager: PackageManager, includeSystemApps: Boolean): List<AppInfo> {
    return withContext(Dispatchers.IO) {
        try {
            val allRelevantApps = mutableSetOf<ApplicationInfo>()

            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launcherActivities = packageManager.queryIntentActivities(launcherIntent, 0)
            val launcherPackages = launcherActivities.mapNotNull { it.activityInfo?.packageName }.toSet()

            launcherActivities.forEach { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo?.packageName
                    if (packageName != null) {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        allRelevantApps.add(appInfo)
                    }
                } catch (_: Exception) {}
            }

            // Add stock launcher
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val homeActivities = packageManager.queryIntentActivities(homeIntent, 0)
            val homePackages = homeActivities.mapNotNull { it.activityInfo?.packageName }.toSet()

            homeActivities.forEach { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo?.packageName
                    if (packageName != null) {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        allRelevantApps.add(appInfo)
                    }
                } catch (_: Exception) {}
            }

            allRelevantApps.mapNotNull { appInfo ->
                try {
                    val packageName = appInfo.packageName

                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

//                    if (!includeSystemApps && isSystemApp) return@mapNotNull null
                    if (appName.isBlank()) return@mapNotNull null

                    val isEnabled = try {
                        appInfo.enabled
                    } catch (e: Exception) {
                        true
                    }
                    if (!isEnabled) return@mapNotNull null

                    AppInfo(
                        packageName = packageName,
                        appName = appName,
                        isSystemApp = isSystemApp,
                        hasLauncherActivity = packageName in launcherPackages,
                        isHomeLauncher = packageName in homePackages
                    )
                } catch (e: Exception) {
                    null
                }
            }
                .distinctBy { it.packageName }
                .sortedWith(
                    compareBy<AppInfo>
//                        { !it.isHomeLauncher }
//                        .thenBy { !it.hasLauncherActivity }
//                        .thenBy { it.isSystemApp }
//                        .thenBy
                        { it.appName.lowercase() }
                )
        } catch (e: Exception) {
            emptyList()
        }
    }
}