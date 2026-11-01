package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AccountEntity
import com.example.data.local.PlaylistConfig
import com.example.viewmodel.IptvViewModel

@Composable
fun SettingsDialog(
    config: PlaylistConfig,
    accounts: List<AccountEntity>,
    viewModel: IptvViewModel,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        SettingsTab("Hesaplar", Icons.Default.AccountCircle),
        SettingsTab("Oynatıcı", Icons.Default.PlayCircle),
        SettingsTab("Ebeveyn", Icons.Default.Lock),
        SettingsTab("Altyazı & EPG", Icons.Default.Subtitles),
        SettingsTab("Hakkında", Icons.Default.Info)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .testTag("settings_dialog_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ZANA IPTV Ayarları",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bağlantı, profil, oynatıcı ve güvenlik tercihleri",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tab.title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> AccountsSection(
                            config = config,
                            accounts = accounts,
                            viewModel = viewModel
                        )
                        1 -> PlayerSection(
                            config = config,
                            viewModel = viewModel
                        )
                        2 -> ParentalSection(
                            config = config,
                            viewModel = viewModel
                        )
                        3 -> SubtitleEpgSection(
                            config = config,
                            viewModel = viewModel
                        )
                        4 -> AboutSection(
                            onLogout = {
                                onDismiss()
                                onLogout()
                            }
                        )
                    }
                }
            }
        }
    }
}

private data class SettingsTab(val title: String, val icon: ImageVector)

@Composable
private fun AccountsSection(
    config: PlaylistConfig,
    accounts: List<AccountEntity>,
    viewModel: IptvViewModel
) {
    var userAgentText by remember { mutableStateOf(config.userAgent) }
    var testUrlInput by remember { mutableStateOf(config.sourceUri.ifBlank { "https://google.com" }) }

    val isPinging by viewModel.isPinging.collectAsState()
    val pingResult by viewModel.pingResult.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Kayıtlı Listeler ve Hesaplar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Aktif olan listeye dokunarak geçiş yapabilirsiniz.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (accounts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = config.name.ifBlank { "Mevcut Playlist" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Kaynak: ${config.type} (${config.sourceUri})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(accounts) { account ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.switchAccount(account) }
                        .testTag("account_item_${account.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (account.isActive)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (account.isActive) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "AKTİF",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Tür: ${account.type} | ${account.sourceUrl}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        if (!account.isActive) {
                            IconButton(onClick = { viewModel.deleteAccount(account.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Sil",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Connection Health Check / Ping
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Bağlantı Sağlığı ve Ping Testi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = testUrlInput,
                onValueChange = { testUrlInput = it },
                label = { Text("Test Edilecek URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.testConnectionHealth(testUrlInput) },
                    enabled = !isPinging,
                    modifier = Modifier.testTag("test_ping_button")
                ) {
                    if (isPinging) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Ediliyor...")
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bağlantıyı Test Et")
                    }
                }

                val currentPing = pingResult
                if (currentPing != null) {
                    val success = currentPing.first
                    val latency = currentPing.second
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (success) Color(0xFF2E7D32).copy(alpha = 0.2f)
                                else Color(0xFFD32F2F).copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (success) "✓ Başarılı (${latency} ms)" else "✗ Bağlanılamadı (${latency} ms)",
                            color = if (success) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Custom User-Agent
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Özel HTTP User-Agent",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Bazı yayın sağlayıcıları belirli User-Agent (VLC, Kodi vb.) gerektirir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = userAgentText,
                    onValueChange = { userAgentText = it },
                    placeholder = { Text("Örn: VLC/3.0.18 LibVLC/3.0.18") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.setUserAgent(userAgentText) },
                    modifier = Modifier.testTag("save_user_agent_button")
                ) {
                    Text("Kaydet")
                }
            }
        }
    }
}

@Composable
private fun PlayerSection(
    config: PlaylistConfig,
    viewModel: IptvViewModel
) {
    val bufferOptions = listOf(5, 15, 30)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Tampon (Buffer) Boyutu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Donmaları önlemek için internet hızınıza göre tampon süresini belirleyin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                bufferOptions.forEach { sec ->
                    val isSelected = config.bufferSeconds == sec
                    OutlinedButton(
                        onClick = { viewModel.setBufferSeconds(sec) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Text("$sec Saniye", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        item {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kaldığım Yerden Devam Et",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Film ve dizilerde izleme pozisyonunu hatırlar ve devam etmeyi sorar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.resumePlaybackEnabled,
                    onCheckedChange = { viewModel.setResumePlaybackEnabled(it) },
                    modifier = Modifier.testTag("resume_toggle_switch")
                )
            }
        }

        item {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sonraki Bölümü Otomatik Başlat",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dizi bölümleri bittiğinde otomatik olarak sıradaki bölüme geçer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.autoNextEpisode,
                    onCheckedChange = { viewModel.setAutoNextEpisode(it) },
                    modifier = Modifier.testTag("auto_next_toggle_switch")
                )
            }
        }
    }
}

@Composable
private fun ParentalSection(
    config: PlaylistConfig,
    viewModel: IptvViewModel
) {
    var pinInput by remember { mutableStateOf(config.parentalPin) }
    var pinSavedMessage by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Ebeveyn Kontrolü & PIN Kilidi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Yetişkin (+18) içeriklere veya kilitli kategorilere erişim için 4 haneli PIN belirleyin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4) pinInput = it },
                    label = { Text("4 Haneli Güvenlik PIN'i") },
                    placeholder = { Text("0000") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        viewModel.setParentalPin(pinInput)
                        pinSavedMessage = true
                    },
                    modifier = Modifier.testTag("save_pin_button")
                ) {
                    Text("Kaydet")
                }
            }

            if (pinSavedMessage) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "✓ PIN başarıyla güncellendi: $pinInput",
                    color = Color(0xFF2E7D32),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Korumalı Kategoriler",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Bu kategorilerdeki kanalları açarken PIN sorulur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            val defaultProtected = listOf("+18", "Adult", "Yetişkin", "Erotik", "Sensörlü")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                defaultProtected.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "🔒 $cat", style = MaterialTheme.typography.bodyMedium)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "KİLİTLİ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleEpgSection(
    config: PlaylistConfig,
    viewModel: IptvViewModel
) {
    var epgUrlInput by remember { mutableStateOf(config.customEpgUrl) }
    val subtitleSizes = listOf(
        Pair("Küçük", 14),
        Pair("Normal", 18),
        Pair("Büyük", 24)
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Özel XMLTV EPG Kaynağı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Elektronik Program Rehberi için özel XMLTV bağlantısı girin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = epgUrlInput,
                    onValueChange = { epgUrlInput = it },
                    placeholder = { Text("https://epg.example.com/epg.xml.gz") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = { viewModel.setCustomEpgUrl(epgUrlInput) },
                    modifier = Modifier.testTag("save_epg_url_button")
                ) {
                    Text("Kaydet")
                }
            }
        }

        item {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Altyazı Font Boyutu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Oynatıcıdaki altyazıların büyüklüğünü ayarlayın.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                subtitleSizes.forEach { option ->
                    val label = option.first
                    val size = option.second
                    val isSelected = config.subtitleSizeSp == size
                    OutlinedButton(
                        onClick = { viewModel.setSubtitleSizeSp(size) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutSection(
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ZANA IPTV v2.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Android TV, TV Box ve Mobil cihazlar için optimize edilmiş profesyonel IPTV & VOD oynatıcısı.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• M3U / M3U8, Xtream Codes ve Stalker Portal desteği\n" +
                                "• 4'lü Çoklu Ekran (Multi-View) & Resim İçinde Resim (PiP)\n" +
                                "• Kumanda Zapping (Yukarı/Aşağı) & Son Kanala Dön (Recall)\n" +
                                "• Film, Dizi & VOD devam etme ve izleme listesi",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("logout_clear_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tüm Verileri Sıfırla ve Çıkış Yap")
            }
        }
    }
}
