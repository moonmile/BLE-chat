package net.moonmile.ble5_chat.claude.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.moonmile.ble5_chat.claude.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val debugTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

// ── 疎通確認画面 ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPingScreen(
    messages: List<ChatMessage>,
    selfId: String,
    onSend: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var currentJob   by remember { mutableStateOf<Job?>(null) }
    var isSending    by remember { mutableStateOf(false) }
    var sendStatus   by remember { mutableStateOf("") }

    // ── 送信ループ ─────────────────────────────────────────────────
    fun startSending(count: Int, intervalMs: Long) {
        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                isSending  = true
                sendStatus = if (count == 1) "送信中..." else "送信中  1 / $count"
                repeat(count) { index ->
                    val seq = index + 1
                    onSend(if (count == 1) "Hello" else "Hello $seq")
                    sendStatus = if (count == 1) "送信完了" else "送信中  $seq / $count"
                    if (seq < count) delay(intervalMs)
                }
                sendStatus = if (count == 1) "送信完了" else "送信完了（$count 件）"
            } finally {
                isSending   = false
                currentJob  = null
            }
        }
    }

    fun stopSending() {
        currentJob?.cancel()
        currentJob  = null
        isSending   = false
        sendStatus  = "中止しました"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("疎通確認") },
                navigationIcon = {
                    IconButton(onClick = { stopSending(); onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor          = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // ── 送信テストセクション ──────────────────────────────
            DebugSectionHeader("送信テスト")

            PingButton(
                label   = "Hello を 1 回送信",
                enabled = !isSending,
                onClick = { startSending(count = 1, intervalMs = 0L) }
            )
            PingButton(
                label   = "Hello + 通番  1 秒おきに 10 回送信",
                enabled = !isSending,
                onClick = { startSending(count = 10, intervalMs = 1_000L) }
            )
            PingButton(
                label   = "Hello + 通番  5 秒おきに  5 回送信",
                enabled = !isSending,
                onClick = { startSending(count = 5,  intervalMs = 5_000L) }
            )

            // ── ステータス行 ──────────────────────────────────────
            if (sendStatus.isNotEmpty() || isSending) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier  = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color     = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text  = sendStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSending) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSending) {
                        OutlinedButton(
                            onClick       = { stopSending() },
                            modifier      = Modifier.height(32.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Text("中止", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            // ── 受信メッセージセクション ──────────────────────────
            DebugSectionHeader("受信メッセージ（${messages.size} 件）")

            if (messages.isEmpty()) {
                Box(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment  = Alignment.Center
                ) {
                    Text(
                        text  = "まだメッセージがありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val listState = rememberLazyListState()
                // 新しいメッセージが来たら末尾へスクロール
                LaunchedEffect(messages.size) {
                    listState.animateScrollToItem(messages.size - 1)
                }
                LazyColumn(
                    state    = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    items(messages, key = { it.messageId }) { msg ->
                        DebugMessageRow(
                            message = msg,
                            isSelf  = msg.senderId == selfId
                        )
                    }
                }
            }
        }
    }
}

// ── 送信ボタン ────────────────────────────────────────────────────
@Composable
private fun PingButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── セクション見出し ───────────────────────────────────────────────
@Composable
private fun DebugSectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelMedium,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
    )
}

// ── 受信メッセージ行 ──────────────────────────────────────────────
@Composable
private fun DebugMessageRow(message: ChatMessage, isSelf: Boolean) {
    val timeStr = debugTimeFormat.format(Date(message.timestamp))
    val bgColor = if (isSelf) MaterialTheme.colorScheme.primaryContainer
                  else        MaterialTheme.colorScheme.surfaceVariant
    val directionLabel = if (isSelf) "→ 送信" else "← 受信"
    val directionColor = if (isSelf) MaterialTheme.colorScheme.primary
                         else        Color(0xFF2E7D32)  // 緑

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 方向ラベル
            Text(
                text      = directionLabel,
                style     = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color     = directionColor,
                modifier  = Modifier.width(44.dp)
            )
            // 時刻
            Text(
                text     = timeStr,
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(60.dp)
            )
            // 送信者 ID
            Text(
                text     = "[${message.senderId}]",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(80.dp)
            )
            // 本文
            Text(
                text  = message.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
