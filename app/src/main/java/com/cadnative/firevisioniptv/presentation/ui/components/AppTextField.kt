package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import com.cadnative.firevisioniptv.presentation.ui.theme.subtleBorder

/**
 * The single text input for the app — search box look everywhere: surface
 * background, [MaterialTheme.shapes] medium corners, a subtle border when idle
 * that lights up to the secondary accent as a focus-highlighted bottom border,
 * and a matching cursor. Callers own width and focus (pass `.weight(1f)` or
 * `.fillMaxWidth()` and any focusRequester via [modifier]).
 *
 * On TV, [dpadEditToggle] keeps the IME hidden until the user presses
 * center/enter on the focused field, so the D-pad can traverse fields without
 * the soft keyboard popping up. Disable it on mobile where focus should raise
 * the keyboard immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    dpadEditToggle: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var editing by remember { mutableStateOf(false) }

    val dpadModifier = if (dpadEditToggle) {
        Modifier
            .onFocusChanged {
                if (it.hasFocus && !editing) keyboardController?.hide()
                if (!it.hasFocus) editing = false
            }
            .onKeyEvent { event ->
                if (!editing &&
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    editing = true
                    keyboardController?.show()
                    true
                } else false
            }
    } else {
        Modifier
    }

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
            unfocusedIndicatorColor = subtleBorder,
            cursorColor = MaterialTheme.colorScheme.secondary
        ),
        modifier = modifier.then(dpadModifier)
    )
}
