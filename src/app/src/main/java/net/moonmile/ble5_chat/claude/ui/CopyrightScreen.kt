package net.moonmile.ble5_chat.claude.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ── ライブラリデータ  §4.4.2 ─────────────────────────────────────
private data class LibraryInfo(
    val name: String, val version: String,
    val license: String, val copyright: String
)

private val libraries = listOf(
    LibraryInfo("Kotlin",                   "2.0.21",          "Apache License 2.0",
        "Copyright 2010–2024 JetBrains s.r.o. and Kotlin Programming Language contributors"),
    LibraryInfo("Kotlinx Coroutines",       "1.8.1",           "Apache License 2.0",
        "Copyright 2016–2024 JetBrains s.r.o."),
    LibraryInfo("Jetpack Compose",          "BOM 2024.09.00",  "Apache License 2.0",
        "Copyright 2019 The Android Open Source Project"),
    LibraryInfo("Compose Material3",        "BOM 2024.09.00",  "Apache License 2.0",
        "Copyright 2021 The Android Open Source Project"),
    LibraryInfo("Navigation Compose",       "2.8.9",           "Apache License 2.0",
        "Copyright 2020 The Android Open Source Project"),
    LibraryInfo("AndroidX Core KTX",        "1.18.0",          "Apache License 2.0",
        "Copyright 2018 The Android Open Source Project"),
    LibraryInfo("AndroidX Lifecycle",       "2.10.0",          "Apache License 2.0",
        "Copyright 2017 The Android Open Source Project"),
    LibraryInfo("AndroidX Activity Compose","1.13.0",          "Apache License 2.0",
        "Copyright 2021 The Android Open Source Project"),
)

// ────────────────────────────────────────────────────────────────
// CopyrightScreen  §4
// ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyrightScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("著作権情報") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor        = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor     = MaterialTheme.colorScheme.onPrimaryContainer,
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
                .padding(16.dp)
        ) {
            // §4.4.1 アプリ情報カード
            AppInfoCard()

            Spacer(modifier = Modifier.height(24.dp))

            // §4.4.2 オープンソースライセンス一覧
            Text("オープンソースライセンス",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp))
            Text("本アプリは以下のオープンソースライブラリを使用しています。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp))

            libraries.forEachIndexed { i, lib ->
                LibraryCard(lib)
                if (i < libraries.lastIndex) Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // §4.4.3 Apache License 2.0 要約カード
            LicenseSummaryCard()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── AppInfoCard  §4.4.1 ───────────────────────────────────────────
@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("BLE5 Chat",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Version 1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
            Text("Copyright © 2025 moonmile",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(4.dp))
            Text("BLE5 Extended Advertising を使った近距離ブロードキャストチャットアプリです。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
    }
}

// ── LibraryCard  §4.4.2 ───────────────────────────────────────────
@Composable
private fun LibraryCard(lib: LibraryInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(lib.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                Text(lib.version,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(lib.license,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(lib.copyright,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── LicenseSummaryCard  §4.4.3 ────────────────────────────────────
@Composable
private fun LicenseSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Apache License 2.0 について",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp))
            Text(
                text = """
Apache License 2.0 は、ソフトウェアの使用・複製・配布・改変・再配布を、条件付きで無償で許可するオープンソースライセンスです。

主な条件:
• 著作権表示およびライセンス本文を保持すること
• 改変した場合は改変した旨を明記すること
• 商用利用・再配布・改変・特許利用を許可

ライセンス全文:
https://www.apache.org/licenses/LICENSE-2.0
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
