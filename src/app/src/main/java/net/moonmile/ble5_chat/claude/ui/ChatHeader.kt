package net.moonmile.ble5_chat.claude.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import net.moonmile.ble5_chat.claude.model.ChatUiState

@Composable
fun ChatHeader(
    state: ChatUiState,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BleStatusIndicator(canSend = state.canSend)
            PeerCountBadge(peerCount = state.peerCount)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScanningIndicator(isScanning = state.isScanning)
                IconButton(onClick = onNavigateToFavorites) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "お気に入り",
                        tint = Color(0xFFFFC107)
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "設定",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun BleStatusIndicator(canSend: Boolean) {
    val dotColor   = if (canSend) Color(0xFF2E7D32) else Color(0xFFC62828)
    val statusText = if (canSend) "Bluetooth ON"   else "Bluetooth OFF"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "●", color = dotColor, style = MaterialTheme.typography.bodyLarge)
        Text(
            text     = " $statusText",
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun PeerCountBadge(peerCount: Int) {
    Text(
        text  = "👥 ${peerCount}人",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@Composable
fun ScanningIndicator(isScanning: Boolean) {
    if (!isScanning) return
    val transition = rememberInfiniteTransition(label = "scan")
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "rotation"
    )
    Text(
        text     = "⟳",
        style    = MaterialTheme.typography.bodyMedium,
        color    = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(end = 4.dp).graphicsLayer { rotationZ = rotation }
    )
}
