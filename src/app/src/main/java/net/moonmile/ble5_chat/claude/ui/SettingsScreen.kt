package net.moonmile.ble5_chat.claude.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.moonmile.ble5_chat.claude.BuildConfig

@Composable
fun SettingsScreen(
    senderId: String,
    onUpdateSenderId: (String) -> Unit,
    onBackToChat: () -> Unit,
    onOpenCopyright: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val versionName = context.appVersionName()
    var editingSenderId by rememberSaveable(senderId) { mutableStateOf(senderId) }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "設定",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            SettingRow(
                title = "アプリ名",
                value = context.applicationInfo.loadLabel(context.packageManager).toString()
            )
            HorizontalDivider()
            SettingRow(
                title = "バージョン",
                value = versionName
            )
            HorizontalDivider()
            SettingRow(
                title = "送信継続時間",
                value = BuildConfig.ADVERTISE_DURATION_MS.toSecondsLabel()
            )
            HorizontalDivider()
            SettingRow(
                title = "重複排除 TTL",
                value = BuildConfig.DUPLICATE_FILTER_TTL_MS.toSecondsLabel()
            )
            HorizontalDivider()
            SettingRow(
                title = "参加者タイムアウト",
                value = BuildConfig.PEER_TIMEOUT_MS.toSecondsLabel()
            )
            HorizontalDivider()
            Text(
                text = "送信者ID",
                style = MaterialTheme.typography.bodyLarge
            )
            OutlinedTextField(
                value = editingSenderId,
                onValueChange = { editingSenderId = it.take(8) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("送信者ID")
                },
                placeholder = {
                    Text("例: A1B2C3D4")
                },
                supportingText = {
                    Text(
                        text = "最大8文字（空欄は保存不可）",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
            Button(
                onClick = { onUpdateSenderId(editingSenderId) },
                enabled = editingSenderId.trim().isNotEmpty() && editingSenderId.trim() != senderId
            ) {
                Text("送信者IDを保存")
            }
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenCopyright)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "コピーライト",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "開く",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider()

            Text(
                text = "チャット画面に戻る",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onBackToChat)
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Context.appVersionName(): String {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName ?: "Unknown"
    } catch (_: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

private fun Long.toSecondsLabel(): String = "${this / 1000}秒"
