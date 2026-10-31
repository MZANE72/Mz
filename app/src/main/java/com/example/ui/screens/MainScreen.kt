package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.example.ui.components.LiveClock
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.RedAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSecondary
import com.example.viewmodel.IptvViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    viewModel: IptvViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.playlistConfig.collectAsState()
    val liveChannels by viewModel.filteredLiveChannels.collectAsState()
    val allLiveChannels by viewModel.rawLiveChannels.collectAsState()
    val movieChannels by viewModel.movieChannels.collectAsState()
    val seriesChannels by viewModel.seriesChannels.collectAsState()
    val selectedCategory by viewModel.selectedLiveCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val currentPlayingChannel by viewModel.currentPlayingChannel.collectAsState()
    val isMultiViewActive by viewModel.isMultiViewActive.collectAsState()
    val multiViewSlots by viewModel.multiViewSlots.collectAsState()
    val activeAudioSlotIndex by viewModel.activeAudioSlotIndex.collectAsState()
    val multiViewMode by viewModel.multiViewMode.collectAsState()

    var selectedNavTab by remember { mutableIntStateOf(0) } // 0: Canlı TV, 1: Filmler, 2: Diziler
    var showLogoutDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR")) }

    // If a channel is currently playing in full screen
    if (currentPlayingChannel != null) {
        PlayerScreen(
            channel = currentPlayingChannel!!,
            onClose = { viewModel.stopPlayback() }
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
            .background(DarkBg),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .border(1.dp, DarkCardBorder)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo and Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(listOf(CyanPrimary, VioletSecondary))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        }

                        Column {
                            Text(
                                text = "NOVA IPTV",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = config.name.ifBlank { "Aktif Playlist" },
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Live Clock & Logout/Switch button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiveClock(compact = true, showDate = true)

                        IconButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1E2638), CircleShape)
                                .testTag("logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Playlist Değiştir / Çıkış",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 3 Günden az kaldıysa UYARI BİLDİRİMİ (ZORUNLU)
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
                                    "Aboneliğinizin bitmesine $daysLeft gün kaldı! ($dateFormatter)"
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
                containerColor = DarkSurface,
                contentColor = CyanPrimary,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkCardBorder)
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
                    label = { Text("Canlı TV", fontWeight = if (selectedNavTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyanPrimary,
                        indicatorColor = CyanPrimary,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_live_tv")
                )

                // 2) Filmler
                NavigationBarItem(
                    selected = selectedNavTab == 1,
                    onClick = { selectedNavTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedNavTab == 1) Icons.Default.Movie else Icons.Outlined.Movie,
                            contentDescription = "Filmler"
                        )
                    },
                    label = { Text("Filmler", fontWeight = if (selectedNavTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyanPrimary,
                        indicatorColor = CyanPrimary,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_movies")
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
                    label = { Text("Diziler", fontWeight = if (selectedNavTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyanPrimary,
                        indicatorColor = CyanPrimary,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
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
                .background(DarkBg)
        ) {
            when (selectedNavTab) {
                0 -> {
                    LiveTvScreen(
                        channels = liveChannels,
                        allChannels = allLiveChannels,
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

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("Playlisti Değiştir", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Mevcut playlist veya hesap bağlantısını kesip yeni bir URL, dosya veya hesap eklemek istiyor musunuz?",
                    color = TextSecondary
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
                    Text("İptal", color = TextMuted)
                }
            }
        )
    }
}
