package com.austinauyeung.nyuma.c9.settings.ui

import KeyCaptureOverlay
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.KeyEvent
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.datastore.core.IOException
import com.austinauyeung.nyuma.c9.common.domain.ScreenEdgeBehavior
import com.austinauyeung.nyuma.c9.core.constants.CursorConstants
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.cursor.domain.IconAlignment
import com.austinauyeung.nyuma.c9.settings.domain.ControlScheme
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

/**
 * Standard cursor settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursorSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCursorKeyCaptureOverlay by remember { mutableStateOf(false) }
    var reservedKeys by remember { mutableStateOf(emptyMap<Int, String>()) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    val inToggleControlScheme = (uiState.controlScheme == ControlScheme.DPAD_TOGGLE || uiState.controlScheme == ControlScheme.NUMPAD_TOGGLE)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(MediaStore.ACTION_PICK_IMAGES).apply {
            type = "image/*"
            putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 1)
        }
    } else {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }

    // Can make more DRY
    val clearIcon = {
        viewModel.updatePreference(null) { settings, v ->
            settings.copy(cursorImagePath = v)
        }
    }
    val clearScrollToggleIcon = {
        viewModel.updatePreference(null) { settings, v ->
            settings.copy(scrollToggleImagePath = v)
        }
    }

    fun clearImage(
        imagePath: String?,
        updateSetting: () -> Unit
    ) {
        if (!imagePath.isNullOrEmpty()) {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
                Logger.d("Custom icon deleted")
            }
        }
        updateSetting()
    }

    val iconPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                coroutineScope.launch {
                    try {
                        clearImage(uiState.cursorImagePath, clearIcon)
                        val savedImagePath = saveImageToAppStorage(context, uri)
                        viewModel.updatePreference(savedImagePath) { settings, v ->
                            settings.copy(cursorImagePath = v)
                        }
                    } catch (e: Exception) {
                        Logger.e("Failed to process cursor image", e)
                    }
                }
            }
        }
    }

    val scrollToggleIconPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                coroutineScope.launch {
                    try {
                        clearImage(uiState.scrollToggleImagePath, clearScrollToggleIcon)
                        val savedImagePath = saveImageToAppStorage(context, uri)
                        viewModel.updatePreference(savedImagePath) { settings, v ->
                            settings.copy(scrollToggleImagePath = v)
                        }
                    } catch (e: Exception) {
                        Logger.e("Failed to process cursor image", e)
                    }
                }
            }
        }
    }

    when (uiState.controlScheme) {
        ControlScheme.STANDARD -> {
            reservedKeys =
                mapOf(
                    KeyEvent.KEYCODE_1 to "Zoom out",
                    KeyEvent.KEYCODE_2 to "Scroll up",
                    KeyEvent.KEYCODE_3 to "Zoom in",
                    KeyEvent.KEYCODE_4 to "Scroll left",
                    KeyEvent.KEYCODE_5 to "Cursor select and double tap",
                    KeyEvent.KEYCODE_6 to "Scroll right",
                    KeyEvent.KEYCODE_7 to "",
                    KeyEvent.KEYCODE_8 to "Scroll down",
                    KeyEvent.KEYCODE_9 to "",
                    KeyEvent.KEYCODE_STAR to "",
                    KeyEvent.KEYCODE_0 to "",
                    KeyEvent.KEYCODE_POUND to "",
                    KeyEvent.KEYCODE_DPAD_UP to "Cursor up",
                    KeyEvent.KEYCODE_DPAD_DOWN to "Cursor down",
                    KeyEvent.KEYCODE_DPAD_LEFT to "Cursor left",
                    KeyEvent.KEYCODE_DPAD_RIGHT to "Cursor right",
                    KeyEvent.KEYCODE_DPAD_CENTER to "Cursor select and double tap",
                )
        }

        ControlScheme.SWAPPED -> {
            reservedKeys =
                mapOf(
                    KeyEvent.KEYCODE_1 to "Zoom out",
                    KeyEvent.KEYCODE_2 to "Cursor up",
                    KeyEvent.KEYCODE_3 to "Zoom in",
                    KeyEvent.KEYCODE_4 to "Cursor left",
                    KeyEvent.KEYCODE_5 to "Cursor select and double tap",
                    KeyEvent.KEYCODE_6 to "Cursor right",
                    KeyEvent.KEYCODE_7 to "",
                    KeyEvent.KEYCODE_8 to "Cursor down",
                    KeyEvent.KEYCODE_9 to "",
                    KeyEvent.KEYCODE_STAR to "",
                    KeyEvent.KEYCODE_0 to "",
                    KeyEvent.KEYCODE_POUND to "",
                    KeyEvent.KEYCODE_DPAD_UP to "Scroll up",
                    KeyEvent.KEYCODE_DPAD_DOWN to "Scroll down",
                    KeyEvent.KEYCODE_DPAD_LEFT to "Scroll left",
                    KeyEvent.KEYCODE_DPAD_RIGHT to "Scroll right",
                    KeyEvent.KEYCODE_DPAD_CENTER to "",
                )
        }

        ControlScheme.DPAD_TOGGLE -> {
            reservedKeys =
                mapOf(
                    KeyEvent.KEYCODE_1 to "Zoom out",
                    KeyEvent.KEYCODE_2 to "",
                    KeyEvent.KEYCODE_3 to "Zoom in",
                    KeyEvent.KEYCODE_4 to "",
                    KeyEvent.KEYCODE_5 to "Cursor select and double tap",
                    KeyEvent.KEYCODE_6 to "",
                    KeyEvent.KEYCODE_7 to "",
                    KeyEvent.KEYCODE_8 to "",
                    KeyEvent.KEYCODE_9 to "",
                    KeyEvent.KEYCODE_STAR to "",
                    KeyEvent.KEYCODE_0 to "",
                    KeyEvent.KEYCODE_POUND to "",
                    KeyEvent.KEYCODE_DPAD_UP to "Cursor up and scroll up",
                    KeyEvent.KEYCODE_DPAD_DOWN to "Cursor down and scroll down",
                    KeyEvent.KEYCODE_DPAD_LEFT to "Cursor left and scroll left",
                    KeyEvent.KEYCODE_DPAD_RIGHT to "Cursor right and scroll right",
                    KeyEvent.KEYCODE_DPAD_CENTER to "Cursor select and double tap",
                )
        }

        ControlScheme.NUMPAD_TOGGLE -> {
            reservedKeys =
                mapOf(
                    KeyEvent.KEYCODE_1 to "Zoom out",
                    KeyEvent.KEYCODE_2 to "Cursor up and scroll up",
                    KeyEvent.KEYCODE_3 to "Zoom in",
                    KeyEvent.KEYCODE_4 to "Cursor left and scroll left",
                    KeyEvent.KEYCODE_5 to "Cursor select and double tap",
                    KeyEvent.KEYCODE_6 to "Cursor right and scroll right",
                    KeyEvent.KEYCODE_7 to "",
                    KeyEvent.KEYCODE_8 to "Cursor down and scroll down",
                    KeyEvent.KEYCODE_9 to "",
                    KeyEvent.KEYCODE_STAR to "",
                    KeyEvent.KEYCODE_0 to "",
                    KeyEvent.KEYCODE_POUND to "",
                    KeyEvent.KEYCODE_DPAD_UP to "",
                    KeyEvent.KEYCODE_DPAD_DOWN to "",
                    KeyEvent.KEYCODE_DPAD_LEFT to "",
                    KeyEvent.KEYCODE_DPAD_RIGHT to "",
                    KeyEvent.KEYCODE_DPAD_CENTER to "Cursor select and double tap",
                )
        }
    }

    val currentKeyDescription =
        if (
            uiState.cursorActivationKey != OverlaySettings.KEY_NONE &&
            !reservedKeys[uiState.cursorActivationKey].isNullOrEmpty()
        ) {
            reservedKeys[uiState.cursorActivationKey]
        } else {
            null
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Standard Cursor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            PreferenceCategory(title = "Activation") {
                if (currentKeyDescription != null) {
                    NoteItem(
                        title = "\"$currentKeyDescription\" overridden and disabled",
                        icon = Icons.Default.Warning,
                        contentDescription = "Warning",
                        color = Color(0xFFFFF4E6),
                    )
                }

                SetKeyPreferenceItem(
                    title = "Set Activation Key",
                    currentKeyCode = uiState.cursorActivationKey,
                    onCaptureKey = {
                        viewModel.requestHideAllOverlays()
                        showCursorKeyCaptureOverlay = true
                    },
                )

                ClearKeyPreferenceItem(
                    mode = "standard cursor",
                    onClearKey = {
                        viewModel.requestHideAllOverlays()
                        viewModel.updateCursorActivationKey(OverlaySettings.KEY_NONE)
                    },
                )

                if (showCursorKeyCaptureOverlay) {
                    KeyCaptureOverlay(
                        restrictedKeys = setOf(uiState.gridActivationKey),
                        reservedKeys = reservedKeys,
                        onKeySelected = { viewModel.updateCursorActivationKey(it) },
                        onDismiss = { showCursorKeyCaptureOverlay = false },
                        showToast = { message -> viewModel.showToast(message) },
                    )
                }
            }

            PreferenceCategory(title = "Behavior") {
                DropdownPreferenceItem(
                    title = "Control Scheme",
                    subtitle =
                    when (uiState.controlScheme) {
                        ControlScheme.STANDARD -> "D-pad moves, numpad scrolls"
                        ControlScheme.SWAPPED -> "D-pad scrolls, numpad moves"
                        ControlScheme.DPAD_TOGGLE -> "D-pad scrolls and moves"
                        else -> "Numpad scrolls and moves"
                    },
                    selectedOption = uiState.controlScheme,
                    options =
                    listOf(
                        ControlScheme.STANDARD to "Standard",
                        ControlScheme.SWAPPED to "Swapped",
                        ControlScheme.DPAD_TOGGLE to "D-pad",
                        ControlScheme.NUMPAD_TOGGLE to "Numpad"
                    ),
                    onOptionSelected = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(controlScheme = v)
                        }
                    },
                )

                DropdownPreferenceItem(
                    title = "Screen Edge Behavior",
                    subtitle =
                    when (uiState.cursorEdgeBehavior) {
                        ScreenEdgeBehavior.NONE -> "Cursor remains at edge"
                        ScreenEdgeBehavior.WRAP_AROUND -> "Cursor wraps to opposite side"
                        ScreenEdgeBehavior.AUTO_SCROLL -> "Cursor slowly scrolls in edge direction"
                    },
                    selectedOption = uiState.cursorEdgeBehavior,
                    options =
                    listOf(
                        ScreenEdgeBehavior.NONE to "None",
                        ScreenEdgeBehavior.WRAP_AROUND to "Wrap",
                        ScreenEdgeBehavior.AUTO_SCROLL to "Scroll"
                    ),
                    onOptionSelected = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(cursorEdgeBehavior = v)
                        }
                    },
                )

                SliderPreferenceItem(
                    title = "Cursor Speed",
                    value = uiState.cursorSpeed.toFloat(),
                    valueRange = CursorConstants.MIN_SPEED.toFloat()..CursorConstants.MAX_SPEED.toFloat(),
                    valueText = uiState.cursorSpeed.toString(),
                    onValueChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(cursorSpeed = v.toInt())
                        }
                    },
                    steps = 8,
                )

                SliderPreferenceItem(
                    title = "Cursor Acceleration",
                    value = uiState.cursorAcceleration.toFloat(),
                    valueRange = CursorConstants.MIN_ACCELERATION.toFloat()..CursorConstants.MAX_ACCELERATION.toFloat(),
                    valueText = "${uiState.cursorAcceleration}${if (uiState.cursorAcceleration == 1) " (no acceleration)" else ""}",
                    onValueChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(cursorAcceleration = v.toInt())
                        }
                    },
                    steps = 8,
                )

                SliderPreferenceItem(
                    title = "Cursor Acceleration Start",
                    value = uiState.cursorAccelerationStart.toFloat(),
                    valueRange = CursorConstants.MIN_ACCELERATION_START.toFloat()..CursorConstants.MAX_ACCELERATION_START.toFloat(),
                    valueText = "${uiState.cursorAccelerationStart} ms",
                    onValueChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(cursorAccelerationStart = v.toLong())
                        }
                    },
                    steps = 8,
                )

                SliderPreferenceItem(
                    title = "Cursor Acceleration Duration",
                    value = uiState.cursorAccelerationDuration.toFloat(),
                    valueRange = CursorConstants.MIN_ACCELERATION_DURATION.toFloat()..CursorConstants.MAX_ACCELERATION_DURATION.toFloat(),
                    valueText = "${uiState.cursorAccelerationDuration} ms",
                    onValueChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(cursorAccelerationDuration = v.toLong())
                        }
                    },
                    steps = 9,
                )

//                SwitchPreferenceItem(
//                    title = "Long Press Hold",
//                    subtitle = "Press both action keys to toggle hold",
//                    checked = uiState.toggleHold,
//                    onCheckedChange = { value ->
//                        viewModel.updatePreference(value) { settings, v ->
//                            settings.copy(toggleHold = v)
//                        }
//                    },
//                )
            }

            PreferenceCategory(title = "Appearance") {
                SliderPreferenceItem(
                    title = "Cursor Size",
                    value = uiState.cursorSize.toFloat(),
                    valueRange = CursorConstants.MIN_SIZE.toFloat()..CursorConstants.MAX_SIZE.toFloat(),
                    valueText = uiState.cursorSize.toString(),
                    onValueChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(cursorSize = v.toInt())
                        }
                    },
                    steps = 8,
                )

                SwitchPreferenceItem(
                    title = "Smooth Cursor Corners",
                    subtitle = "Round out the corners of the cursor",
                    checked = uiState.roundedCursorCorners,
                    onCheckedChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(roundedCursorCorners = v)
                        }
                    },
                )

                SimplePreferenceItem(
                    title = "Cursor Color",
                    subtitle = "Current hex value: #${uiState.standardCursorHex}",
                    onClick = { showColorPickerDialog = true }
                )

                SwitchPreferenceItem(
                    title = "Match Border to Body",
                    subtitle = "Replace black border and match cursor body color",
                    checked = uiState.standardCursorMatchBorder,
                    onCheckedChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(standardCursorMatchBorder = v)
                        }
                    },
                )

                if (showColorPickerDialog) {
                    ColorPickerDialog(
                        initialColorHex = uiState.standardCursorHex,
                        onColorSelected = { newColorHex ->
                            viewModel.updatePreference(newColorHex) { settings, v ->
                                settings.copy(standardCursorHex = v)
                            }
                        },
                        onDismiss = { showColorPickerDialog = false }
                    )
                }
            }

            PreferenceCategory(title = "Custom Icon") {
                SwitchPreferenceItem(
                    title = "Custom Cursor Icons",
                    subtitle = "Replace the default cursor icon with an image or gif",
                    checked = uiState.useCustomCursorIcon,
                    onCheckedChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(useCustomCursorIcon = v)
                        }
                    },
                )

                SimplePreferenceItem(
                    title = if (!uiState.cursorImagePath.isNullOrEmpty() && File(uiState.cursorImagePath!!).exists() ) "Change Cursor Icon" else "Set Cursor Icon",
                    subtitle = "Supported formats: png, gif, jpg, bmp, webp",
                    onClick = { iconPicker.launch(intent) },
                    enabled = uiState.useCustomCursorIcon
                )
                DropdownPreferenceItem(
                    title = "Cursor Icon Alignment",
                    subtitle =
                    when (uiState.cursorImageAlignment) {
                        IconAlignment.TOP_LEFT -> "Align to top-left of icon"
                        IconAlignment.CENTER -> "Align to center of icon"
                    },
                    selectedOption = uiState.cursorImageAlignment,
                    options =
                    listOf(
                        IconAlignment.TOP_LEFT to "Top left",
                        IconAlignment.CENTER to "Center"
                    ),
                    onOptionSelected = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(cursorImageAlignment = v)
                        }
                    },
                    enabled = uiState.useCustomCursorIcon
                )
                SimplePreferenceItem(
                    title = "Clear Cursor Icon",
                    subtitle = "Fallback to default icon",
                    onClick = { clearImage(uiState.cursorImagePath, clearIcon) },
                    enabled = uiState.useCustomCursorIcon
                )

                SimplePreferenceItem(
                    title = if (!uiState.scrollToggleImagePath.isNullOrEmpty() && File(uiState.scrollToggleImagePath!!).exists() ) "Change Scroll Toggle Icon" else "Set Scroll Toggle Icon",
                    subtitle = if (!inToggleControlScheme) "Applies only in D-pad and numpad control schemes" else "Supported formats: png, gif, jpg, bmp, webp",
                    onClick = { scrollToggleIconPicker.launch(intent) },
                    enabled = uiState.useCustomCursorIcon && inToggleControlScheme
                )
                DropdownPreferenceItem(
                    title = "Scroll Toggle Icon Alignment",
                    subtitle =
                    when (uiState.scrollToggleImageAlignment) {
                        IconAlignment.TOP_LEFT -> "Align to top-left of icon"
                        IconAlignment.CENTER -> "Align to center of icon"
                    },
                    selectedOption = uiState.scrollToggleImageAlignment,
                    options =
                    listOf(
                        IconAlignment.TOP_LEFT to "Top left",
                        IconAlignment.CENTER to "Center"
                    ),
                    onOptionSelected = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(scrollToggleImageAlignment = v)
                        }
                    },
                    enabled = uiState.useCustomCursorIcon && inToggleControlScheme
                )
                SimplePreferenceItem(
                    title = "Clear Scroll Toggle Icon",
                    subtitle = "Fallback to default screen border indicator",
                    onClick = { clearImage(uiState.scrollToggleImagePath, clearScrollToggleIcon) },
                    enabled = uiState.useCustomCursorIcon && inToggleControlScheme
                )
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColorHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var colorHex by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialColorHex,
                selection = TextRange(initialColorHex.length)
            )
        )
    }
    var isError by remember { mutableStateOf(false) }

    val previewColor = try {
        Color(android.graphics.Color.parseColor("#${colorHex.text}"))
        isError = false
        Color(android.graphics.Color.parseColor("#${colorHex.text}"))
    } catch (e: Exception) {
        isError = true
        Color.Black
    }

    val saveAction = {
        if (!isError) {
            onColorSelected(colorHex.text)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cursor Color") },
        text = {
            Column {
                Text("Enter a hex value. A preview is shown on the right.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { input ->
                        val filtered = input.text.filter {
                            it.isDigit() || it in 'A'..'F' || it in 'a'..'f'
                        }.take(6)

                        val newSelection = TextRange(
                            start = minOf(filtered.length, input.selection.start),
                            end = minOf(filtered.length, input.selection.end)
                        )

                        colorHex = TextFieldValue(
                            text = filtered,
                            selection = newSelection,
                            composition = input.composition
                        )
                        try {
                            Color(android.graphics.Color.parseColor("#$filtered"))
                            isError = false
                        } catch (e: Exception) {
                            isError = true
                        }
                    },
                    label = { Text("Hex value") },
                    singleLine = true,
                    isError = isError,
                    prefix = { Text("#") },
                    supportingText = {
                        if (isError) {
                            Text(
                                text = "Invalid hex code",
                                color = Color.Red,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        if (!isError) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(previewColor)
                                    .border(2.dp, Color.Black)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isError) {
                                saveAction()
                            }
                        }
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isError) saveAction()
                },
                enabled = !isError
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

suspend fun saveImageToAppStorage(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val mimeType = context.contentResolver.getType(uri)

    val extension = MimeTypeMap.getSingleton()
        .getExtensionFromMimeType(mimeType)
        ?.lowercase(Locale.getDefault())
        ?.takeIf {
            it in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        } ?: run {
        val uriPath = uri.path
        uriPath?.substringAfterLast('.')?.lowercase(Locale.getDefault())?.takeIf {
            it in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        } ?: "png"
    }

    val fileName = "cursor_${UUID.randomUUID()}.$extension"
    val file = File(context.filesDir, fileName)

    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: IOException) {
        Logger.e("Failed to save image from uri $uri to file ${file.name}", e)
        throw e
    }

    return@withContext file.absolutePath
}