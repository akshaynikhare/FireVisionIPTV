package com.cadnative.firevisioniptv.presentation.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cadnative.firevisioniptv.presentation.ui.LocalPerfProfile
import com.cadnative.firevisioniptv.presentation.ui.animation.DURATION_FAST
import com.cadnative.firevisioniptv.presentation.ui.animation.EaseOutQuart
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HINT_DISPLAY_MS = 2000L

/**
 * Mic button that runs on-device speech recognition and returns the final
 * recognized text via [onResult]. Renders nothing when the device has no
 * recognition service; dims itself for the session if RECORD_AUDIO is denied.
 *
 * [onStatusChange] receives short-lived hint text ("Listening…", error hints)
 * or null when idle.
 */
@Composable
fun VoiceSearchButton(
    onResult: (String) -> Unit,
    onStatusChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recognitionAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    if (!recognitionAvailable) return

    val reduceMotion = LocalPerfProfile.current.reduceMotion
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnStatusChange by rememberUpdatedState(onStatusChange)
    val scope = rememberCoroutineScope()

    var isListening by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var hintJob by remember { mutableStateOf<Job?>(null) }

    fun showHint(text: String) {
        hintJob?.cancel()
        hintJob = scope.launch {
            currentOnStatusChange(text)
            delay(HINT_DISPLAY_MS)
            currentOnStatusChange(null)
        }
    }

    fun startListening() {
        hintJob?.cancel()
        val speech = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { created ->
            created.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onError(error: Int) {
                    isListening = false
                    if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        permissionDenied = true
                        currentOnStatusChange(null)
                    } else {
                        showHint("Didn't catch that — try again")
                    }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    hintJob?.cancel()
                    currentOnStatusChange(null)
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!text.isNullOrBlank()) currentOnResult(text)
                }
            })
            recognizer = created
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        isListening = true
        currentOnStatusChange("Listening…")
        speech.startListening(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening() else permissionDenied = true
    }

    DisposableEffect(Unit) {
        onDispose {
            recognizer?.destroy()
        }
    }

    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = if (reduceMotion) snap() else tween(DURATION_FAST, easing = EaseOutQuart),
        label = "micFocusScale"
    )
    val pulseScale = if (isListening && !reduceMotion) {
        rememberInfiniteTransition(label = "micPulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "micPulseScale"
        ).value
    } else 1f

    Box(
        modifier = modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = focusScale * pulseScale
                scaleY = focusScale * pulseScale
            }
            .alpha(if (permissionDenied) 0.4f else 1f)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (isListening) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.secondary else subtleBorder,
                shape = MaterialTheme.shapes.medium
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                enabled = !permissionDenied,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                when {
                    isListening -> {
                        recognizer?.cancel()
                        isListening = false
                        currentOnStatusChange(null)
                    }
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED -> startListening()
                    else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = if (isListening) "Stop voice search" else "Voice search",
            tint = if (isListening) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimens.IconMedium)
        )
    }
}
