package net.moonmile.ble5_chat.claude.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import net.moonmile.ble5_chat.claude.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

/** お気に入り済み ★ の色 */
private val FavoriteColor = Color(0xFFFFC107)

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    selfId: String,
    favoriteIds: Set<String>,
    onToggleFavorite: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (messages.isEmpty()) {
            Text(
                text  = "メッセージがありません",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                state    = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                reverseLayout = false
            ) {
                items(messages, key = { it.messageId }) { message ->
                    MessageBubble(
                        message          = message,
                        isSelf           = message.senderId == selfId,
                        isFavorite       = message.messageId in favoriteIds,
                        onToggleFavorite = { onToggleFavorite(message) }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isSelf: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    val maxWidth = LocalConfiguration.current.screenWidthDp.dp * 0.75f
    val timeStr  = timeFormat.format(Date(message.timestamp))

    val bubbleShape = if (isSelf) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 12.dp)
    }
    val bubbleColor = if (isSelf) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment     = Alignment.Bottom
    ) {
        // 自分のメッセージ: ★ を吹き出しの左に表示
        if (isSelf) {
            FavoriteIconButton(isFavorite = isFavorite, onClick = onToggleFavorite)
        }

        Column(horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start) {
            if (isSelf) {
                Text(
                    text     = timeStr,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else {
                Text(
                    text     = "${message.senderId}  $timeStr",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            Surface(
                shape    = bubbleShape,
                color    = bubbleColor,
                modifier = Modifier.widthIn(max = maxWidth)
            ) {
                Text(
                    text     = message.text,
                    style    = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // 相手のメッセージ: ★ を吹き出しの右に表示
        if (!isSelf) {
            FavoriteIconButton(isFavorite = isFavorite, onClick = onToggleFavorite)
        }
    }
}

@Composable
private fun FavoriteIconButton(isFavorite: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick  = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector        = Icons.Filled.Star,
            contentDescription = if (isFavorite) "お気に入りから削除" else "お気に入りに追加",
            tint               = if (isFavorite) FavoriteColor
                                 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier           = Modifier.size(18.dp)
        )
    }
}
