package net.moonmile.ble5_chat.claude.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import net.moonmile.ble5_chat.claude.ble.MessageCodec

@Composable
fun MessageInputBar(
    inputText: String,
    canSend: Boolean,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remaining = MessageCodec.MAX_TEXT_LENGTH - inputText.length
    val canSubmit = canSend && inputText.isNotBlank()

    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChange,
                enabled = canSend,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = if (canSend) "メッセージを入力" else "Bluetoothをオンにしてください",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSubmit) onSendClick() }),
                supportingText = {
                    Text(
                        text = "残り $remaining 文字",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (remaining < 10) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            IconButton(
                onClick = onSendClick,
                enabled = canSubmit,
                modifier = Modifier.padding(start = 4.dp, bottom = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "送信",
                    tint = if (canSubmit) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}
