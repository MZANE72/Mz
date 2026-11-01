package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.LocalMovies
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlaylistConfig
import com.example.ui.components.LiveClock
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.RedAccent
import com.example.ui.theme.VioletSecondary
import com.example.viewmodel.IptvViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MainScreen(
    viewModel: IptvViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.playlistConfig.collectAsState()
    val accounts by viewModel.accountsList.collectAsState()
    val liveChannels by viewModel.filteredLiveChannels.collectAsState()
    val allLiveChannels by viewModel.rawLiveChannels.collectAsState()
    val recentLiveChannels by viewModel.recentLiveChannels.collectAsState()
    val movieChannels by viewModel.movieChannels.collectAsState()
    val seriesChannels by viewModel.seriesChannels.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val selectedCategory by viewModel.selectedLiveCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val currentPlayingChannel by viewModel.currentPlayingChannel.collectAsState()
    val isMultiViewActive by viewModel.isMultiViewActive.collectAsState()
    val multiViewSlots by viewModel.multiViewSlots.collectAsState()
    val activeAudioSlotIndex by viewModel.activeAudioSlotIndex.collectAsState()
    val multiViewMode by viewModel.multiViewMode.collectAsState()

    var selectedNavTab by remember { mutableIntStateOf(0) } // 0: Canlı TV, 1: VOD (Filmler), 2: Diziler
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR")) }

    // If a channel is currently playing in full screen
    if (currentPlayingChannel != null) {
        PlayerScreen(
            channel = currentPlayingChannel!!,
            onClose = { viewModel.stopPlayback() },
            onNextChannel = { viewModel.playNextChannel() },
            onPrevChannel = { viewModel.playPreviousChannel() },
            onRecallChannel = { viewModel.recallLastChannel() },
            onSaveResumePosition = { id, pos, dur ->
                viewModel.updateResumePosition(id, pos, dur)
            }
        )
        return
    }

    // If multi-view is active
    if (isMultiViewActive) {
        MultiViewScreen(
            slots = multiViewSlots,
            activeAudioIndex = activeAudioSlotIndex,
            multiViewMode = multiViewMode,
            allLiveChannels = allLiveChannels,
            onSlotTapped = { slotIdx -> viewModel.setActiveAudioSlot(slotIdx) },
            onSelectChannelForSlot = { slotIdx, ch -> viewModel.setMultiViewSlotChannel(slotIdx, ch) },
            onSetMode = { mode -> viewModel.setMultiViewMode(mode) },
            onClose = { viewModel.closeMultiView() }
        )
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo and Title (ZANA IPTV)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(listOf(CyanPrimary, VioletSecondary))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Tv,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "ZANA IPTV",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = config.name.ifBlank { "Aktif Playlist" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Live Clock, Theme Switcher, Settings & Logout
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiveClock(compact = true, showDate = true)

                        // Settings Icon (Bağlantı & Ayarlar)
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ayarlar",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Karanlık veya Sistem Teması Seçeneği
                        IconButton(
                            onClick = {
                                val nextMode = if (config.themeMode == PlaylistConfig.THEME_DARK) {
                                    PlaylistConfig.THEME_SYSTEM
                                } else {
                                    PlaylistConfig.THEME_DARK
                                }
                                viewModel.setThemeMode(nextMode)
                                scope.launch {
                                    val modeName = if (nextMode == PlaylistConfig.THEME_DARK) "Karanlık Tema" else "Sistem Teması"
                                    snackbarHostState.showSnackbar("Tema: $modeName")
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (config.themeMode == PlaylistConfig.THEME_DARK) Icons.Default.DarkMode else Icons.Default.SettingsBrightness,
                                contentDescription = "Tema Değiştir",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Logout button
                        IconButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Playlist Değiştir / Çıkış",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Expiration Warning Banner
                val daysLeft = config.daysLeft()
                val isNearExpiry = config.isNearExpiration()

                if (isNearExpiry && daysLeft != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AmberAccent.copy(alpha = 0.18f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (daysLeft == 0L) {
                                    "Aboneliğiniz BUGÜN sona eriyor! Lütfen süreyi yenileyin."
                                } else {
                                    "Aboneliğinizin bitmesine $daysLeft gün kaldı!"
                                },
                                color = AmberAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                // 1) Canlı TV
                NavigationBarItem(
                    selected = selectedNavTab == 0,
                    onClick = { selectedNavTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedNavTab == 0) Icons.Default.LiveTv else Icons.Outlined.LiveTv,
                            contentDescription = "Canlı TV"
                        )
                    },
                    label = {
                        Text(
                            text = "Canlı TV",
                            fontWeight = if (selectedNavTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_live_tv")
                )

                // 2) AYRI VOD BÖLÜMÜ (Filmler)
                NavigationBarItem(
                    selected = selectedNavTab == 1,
                    onClick = { selectedNavTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedNavTab == 1) Icons.Default.LocalMovies else Icons.Outlined.LocalMovies,
                            contentDescription = "VOD (Filmler)"
                        )
                    },
                    label = {
                        Text(
                            text = "VOD (Filmler)",
                            fontWeight = if (selectedNavTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_vod")
                )

                // 3) Diziler
                NavigationBarItem(
                    selected = selectedNavTab == 2,
                    onClick = { selectedNavTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedNavTab == 2) Icons.Default.VideoLibrary else Icons.Outlined.VideoLibrary,
                            contentDescription = "Diziler"
                        )
                    },
                    label = {
                        Text(
                            text = "Diziler",
                            fontWeight = if (selectedNavTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_series")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedNavTab) {
                0 -> {
                    LiveTvScreen(
                        channels = liveChannels,
                        allChannels = allLiveChannels,
                        recentChannels = recentLiveChannels,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        onSelectCategory = { viewModel.selectLiveCategory(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onPlayChannel = { viewModel.playChannel(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDeleteChannel = { ch ->
                            viewModel.deleteChannel(ch)
                            scope.launch {
                                snackbarHostState.showSnackbar("Kanal gizlendi: ${ch.name}")
                            }
                        },
                        onRenameChannel = { id, newName ->
                            viewModel.renameChannel(id, newName)
                            scope.launch {
                                snackbarHostState.showSnackbar("Kanal adı güncellendi")
                            }
                        },
                        onMoveUp = { viewModel.moveChannelUp(it) },
                        onMoveDown = { viewModel.moveChannelDown(it) },
                        onRestoreDeleted = {
                            viewModel.restoreDeletedChannels()
                            scope.launch {
                                snackbarHostState.showSnackbar("Tüm gizlenen kanallar geri yüklendi")
                            }
                        },
                        onOpenMultiView = { viewModel.openMultiView() }
                    )
                }

                1 -> {
                    // Dedicated VOD Section
                    MoviesScreen(
                        movies = movieChannels,
                        onPlayMovie = { viewModel.playChannel(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
                }

                2 -> {
                    SeriesScreen(
                        seriesList = seriesChannels,
                        onPlayEpisode = { viewModel.playChannel(it) }
                    )
                }
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            config = config,
            accounts = accounts,
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false },
            onLogout = {
                showSettingsDialog = false
                viewModel.logout()
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("Playlisti Değiştir", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Mevcut playlist veya hesap bağlantısını kesip yeni bir URL, dosya veya hesap eklemek istiyor musunuz?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    }
                ) {
                    Text("Evet, Çıkış Yap", color = RedAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
