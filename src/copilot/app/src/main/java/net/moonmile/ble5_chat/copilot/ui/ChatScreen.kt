package net.moonmile.ble5_chat.copilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.moonmile.ble5_chat.copilot.model.ChatUiState

/**
 * チャット画面（ポートレート固定）
 *
 * @param state UI 状態
 * @param onSend 送信ボタン押下時のコールバック
 * @param onStart チャット開始ボタン押下時のコールバック
 * @param onStop チャット停止ボタン押下時のコールバック
 * @param onInputChange テキスト入力変更時のコールバック
 * @param selfId 自分の端末 ID
 * @param modifier レイアウト修飾子
 */
@Composable
fun ChatScreen(
    state: ChatUiState,
    onSend: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onInputChange: (String) -> Unit,
    selfId: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ヘッダ
        ChatHeader(
            canSend = state.canSend,
            isScanning = state.isScanning,
            peerCount = state.peerCount,
            onStart = onStart,
            onStop = onStop
        )

        // メッセージ一覧
        MessageList(
            messages = state.messages,
            selfId = selfId,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // 入力領域
        MessageInputBar(
            inputText = state.inputText,
            canSend = state.canSend,
            onTextChange = onInputChange,
            onSendClick = onSend,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * ヘッダ部分
 */
@Composable
private fun ChatHeader(
    canSend: Boolean,
    isScanning: Boolean,
    peerCount: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BLE 状態
            BleStatusIndicator(canSend = canSend)

            // 参加者数
            Text(
                text = "👥 $peerCount 人",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 24.dp)
            )

            // スキャン中インジケータ
            if (isScanning) {
                Text(
                    text = "⟳ スキャン中",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }

            // 開始/停止ボタン
            if (canSend) {
                IconButton(onClick = onStop, modifier = Modifier.padding(start = 16.dp)) {
                    Text("停止")
                }
            } else {
                IconButton(onClick = onStart, modifier = Modifier.padding(start = 16.dp)) {
                    Text("開始")
                }
            }
        }
    }
}

/**
 * BLE 状態インジケータ
 */
@Composable
private fun BleStatusIndicator(canSend: Boolean) {
    val status = if (canSend) "ON" else "OFF"
    val statusColor = if (canSend) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.error
    }
    val statusIcon = if (canSend) "●" else "●"

    Text(
        text = "$statusIcon $status",
        style = MaterialTheme.typography.labelLarge,
        color = statusColor
    )
}

/**
 * メッセージ一覧
 */
@Composable
private fun MessageList(
    messages: List<net.moonmile.ble5_chat.copilot.model.ChatMessage>,
    selfId: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.padding(8.dp),
        reverseLayout = false
    ) {
        items(messages.size) { index ->
            MessageBubble(
                message = messages[index],
                isSelf = messages[index].senderId == selfId
            )
        }
    }
}

/**
 * メッセージバブル
 */
@Composable
private fun MessageBubble(
    message: net.moonmile.ble5_chat.copilot.model.ChatMessage,
    isSelf: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isSelf) {
            androidx.compose.foundation.layout.Arrangement.End
        } else {
            androidx.compose.foundation.layout.Arrangement.Start
        }
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (isSelf) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isSelf) 12.dp else 4.dp,
                        bottomEnd = if (isSelf) 4.dp else 12.dp
                    )
                )
                .fillMaxWidth(0.75f)
                .padding(8.dp)
        ) {
            // 送信者情報
            val timeStr = android.text.format.DateFormat.format("HH:mm", message.timestamp)
            if (isSelf) {
                Text(
                    text = timeStr.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "${message.senderId} $timeStr",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // メッセージ本文
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * 入力領域
 */
@Composable
private fun MessageInputBar(
    inputText: String,
    canSend: Boolean,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = inputText,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            enabled = canSend,
            singleLine = false,
            placeholder = {
                Text(
                    if (canSend) {
                        "メッセージを入力..."
                    } else {
                        "Bluetooth をオンにしてください"
                    }
                )
            }
        )

        IconButton(
            onClick = onSendClick,
            enabled = canSend && inputText.isNotBlank(),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "送信"
            )
        }
    }
}
