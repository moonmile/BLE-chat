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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import net.moonmile.ble5_chat.claude.model.ChatUiState

@Composable
fun ChatHeader(
    state: ChatUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BleStatusIndicator(canSend = state.canSend)
            PeerCountBadge(peerCount = state.peerCount)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScanningIndicator(isScanning = state.isScanning)
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "メニュー"
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("設定") },
                        onClick = {
                            menuExpanded = false
                            onOpenSettings()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BleStatusIndicator(canSend: Boolean) {
    val color = if (canSend) Color(0xFF2E7D32) else Color(0xFFC62828)
    val statusText = if (canSend) "Bluetooth ON" else "Bluetooth OFF"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "●", color = color, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = " $statusText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun PeerCountBadge(peerCount: Int) {
    Text(
        text = "👥 ${peerCount}人",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@Composable
fun ScanningIndicator(isScanning: Boolean) {
    if (!isScanning) {
        Text(text = "  ", style = MaterialTheme.typography.bodySmall)
        return
    }
    val transition = rememberInfiniteTransition(label = "scan")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "rotation"
    )
    Text(
        text = "⟳ スキャン中",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.graphicsLayer { rotationZ = rotation }
    )
}
