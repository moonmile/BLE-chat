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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.moonmile.ble5_chat.claude.BuildConfig

private data class LibraryInfo(
    val name: String,
    val version: String,
    val license: String,
    val copyright: String
)

private val libraries = listOf(
    LibraryInfo(
        name = "Kotlin",
        version = "2.0.21",
        license = "Apache License 2.0",
        copyright = "Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors"
    ),
    LibraryInfo(
        name = "Kotlinx Coroutines",
        version = "1.8.1",
        license = "Apache License 2.0",
        copyright = "Copyright 2016-2024 JetBrains s.r.o."
    ),
    LibraryInfo(
        name = "Jetpack Compose",
        version = "BOM 2024.09.00",
        license = "Apache License 2.0",
        copyright = "Copyright 2019 The Android Open Source Project"
    ),
    LibraryInfo(
        name = "Compose Material3",
        version = "BOM 2024.09.00",
        license = "Apache License 2.0",
        copyright = "Copyright 2021 The Android Open Source Project"
    ),
    LibraryInfo(
        name = "Navigation Compose",
        version = "2.8.9",
        license = "Apache License 2.0",
        copyright = "Copyright 2020 The Android Open Source Project"
    ),
    LibraryInfo(
        name = "AndroidX Core KTX",
        version = "1.18.0",
        license = "Apache License 2.0",
        copyright = "Copyright 2018 The Android Open Source Project"
    ),
    LibraryInfo(
        name = "AndroidX Lifecycle",
        version = "2.10.0",
        license = "Apache License 2.0",
        copyright = "Copyright 2017 The Android Open Source Project"
    ),
    LibraryInfo(
        name = "AndroidX Activity Compose",
        version = "1.13.0",
        license = "Apache License 2.0",
        copyright = "Copyright 2021 The Android Open Source Project"
    )
)

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
                .padding(16.dp)
        ) {
            AppInfoCard()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "オープンソースライセンス",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "本アプリは以下のオープンソースライブラリを使用しています。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            libraries.forEachIndexed { index, library ->
                LibraryCard(library)
                if (index < libraries.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LicenseSummaryCard()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "BLE5 Chat",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
            )
            Text(
                text = "Copyright © 2025 moonmile",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "BLE5 Extended Advertising を使った近距離ブロードキャストチャットアプリです。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun LibraryCard(library: LibraryInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = library.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = library.version,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = library.license,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = library.copyright,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
private fun LicenseSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Apache License 2.0 について",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
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