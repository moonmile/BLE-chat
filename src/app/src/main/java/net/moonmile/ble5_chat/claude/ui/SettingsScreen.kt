package net.moonmile.ble5_chat.claude.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.moonmile.ble5_chat.claude.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selfId: String,
    onSelfIdChange: (String) -> Unit,
    onNavigateToCopyright: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                .verticalScroll(rememberScrollState())
        ) {
            // ── 端末設定セクション ──────────────────────────────────
            SettingsSectionHeader(title = "端末設定")

            SettingsItem(
                label = "送信者ID",
                value = selfId,
                trailingIcon = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "編集",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── BLE 設定セクション ─────────────────────────────────
            SettingsSectionHeader(title = "BLE 設定")

            SettingsItem(
                label = "送信継続時間",
                value = "${BuildConfig.ADVERTISE_DURATION_SEC} 秒"
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsItem(
                label = "重複排除 TTL",
                value = "${BuildConfig.DUPLICATE_FILTER_TTL_SEC} 秒"
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsItem(
                label = "参加者タイムアウト",
                value = "${BuildConfig.PEER_TIMEOUT_SEC} 秒"
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── アプリ情報セクション ────────────────────────────────
            SettingsSectionHeader(title = "アプリ情報")

            SettingsItem(
                label = "バージョン",
                value = "1.0.0"
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsNavigationItem(
                label = "著作権情報",
                onClick = onNavigateToCopyright
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 送信者ID 編集ダイアログ
    if (showEditDialog) {
        SelfIdEditDialog(
            currentId = selfId,
            onConfirm = { newId ->
                onSelfIdChange(newId)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    label: String,
    value: String,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailingIcon?.invoke()
    }
}

@Composable
private fun SettingsNavigationItem(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SelfIdEditDialog(
    currentId: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentId) }
    val isValid = text.isNotBlank() && text.length <= 8

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("送信者IDを変更") },
        text = {
            Column {
                Text(
                    text = "半角英数字・8文字以内で入力してください。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 8) text = it },
                    singleLine = true,
                    isError = !isValid,
                    supportingText = {
                        if (!isValid) Text("1〜8文字で入力してください")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onConfirm(text) },
                enabled = isValid
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}
