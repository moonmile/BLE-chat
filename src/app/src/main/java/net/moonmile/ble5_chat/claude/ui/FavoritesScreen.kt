package net.moonmile.ble5_chat.claude.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.moonmile.ble5_chat.claude.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val favDateFormat    = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
private val DeleteButtonWidth = 72.dp

// ── お気に入り画面 ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favorites: List<ChatMessage>,
    onRemove: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("お気に入り  (${favorites.size}件)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "お気に入りはまだありません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(favorites, key = { it.messageId }) { msg ->
                    SwipeToRevealDeleteItem(
                        onRemove = { onRemove(msg.messageId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .animateItem()
                    ) {
                        FavoriteItem(message = msg)
                    }
                }
            }
        }
    }
}

// ── スライドで削除ボタンを表示するコンテナ ─────────────────────────
//
// 左スワイプ → 右端に赤いゴミ箱ボタンが現れる
// ゴミ箱アイコンをタップ → onRemove() で削除
// スワイプを戻す（右方向） → 元の位置に戻る
@Composable
private fun SwipeToRevealDeleteItem(
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density        = LocalDensity.current
    val deleteWidthPx  = with(density) { DeleteButtonWidth.toPx() }
    val snapThreshold  = deleteWidthPx * 0.4f   // 40% 超えたら開く
    val velocityLimit  = 500f                    // この速度以上なら開く

    val offsetX = remember { Animatable(0f) }
    val scope   = rememberCoroutineScope()

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            // 左方向のみ受け付け（0 ～ -deleteWidthPx）
            val target = (offsetX.value + delta).coerceIn(-deleteWidthPx, 0f)
            offsetX.snapTo(target)
        }
    }

    Box(modifier = modifier) {

        // ── 背景：削除ボタン（右端に固定） ──────────────────────────
        // matchParentSize() で前景と同じ高さに揃える
        Box(
            modifier         = Modifier.matchParentSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(DeleteButtonWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector        = Icons.Default.Delete,
                        contentDescription = "お気に入りから削除",
                        tint               = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // ── 前景：スライドするコンテンツ ───────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state       = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        scope.launch {
                            // スナップ先を決定：開く or 閉じる
                            val shouldOpen =
                                (-offsetX.value > snapThreshold) || (velocity < -velocityLimit)
                            val snapTarget = if (shouldOpen) -deleteWidthPx else 0f
                            offsetX.animateTo(
                                targetValue = snapTarget,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            )
                        }
                    }
                )
        ) {
            content()
        }
    }
}

// ── メッセージカード ──────────────────────────────────────────────
@Composable
private fun FavoriteItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = message.senderId,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text  = favDateFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text     = message.text,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
