import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * Renders UI for capturing activation key.
 */
@Composable
fun KeyCaptureOverlay(
    restrictedKeys: Set<Int>,
    reservedKeys: Map<Int, String>,
    onKeySelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    showToast: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var timeoutSecondsRemaining by remember { mutableFloatStateOf(10.0f) }
    val focusRequester = remember { FocusRequester() }
    val progress = timeoutSecondsRemaining / 10f
    val displaySeconds = timeoutSecondsRemaining.toInt().coerceAtLeast(0)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        val totalDuration = 10000L

        while (true) {
            val elapsedTime = System.currentTimeMillis() - startTime

            if (elapsedTime >= totalDuration) {
                Toast.makeText(context, "No key set", Toast.LENGTH_SHORT).show()
                onDismiss()
                break
            }

            timeoutSecondsRemaining = (totalDuration - elapsedTime) / 1000f
            delay(16)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Card(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier =
                Modifier
                    .padding(24.dp)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                            when (val keyCode = keyEvent.nativeKeyEvent.keyCode) {
                                in restrictedKeys -> showToast(
                                    "Invalid activation key: ${
                                        KeyEvent.keyCodeToString(
                                            keyCode
                                        )
                                    }"
                                )

                                in reservedKeys.keys -> {
                                    onKeySelected(keyCode)
                                    showToast("Overriding reserved key")
                                }

                                else -> {
                                    onKeySelected(keyCode)
                                    showToast("Activation key set")
                                }
                            }

                            onDismiss()
                            true
                        } else {
                            false
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Waiting for key press...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(56.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 6.dp,
                    )

                    Text(
                        text = "$displaySeconds",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
