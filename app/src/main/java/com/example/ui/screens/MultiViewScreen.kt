package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.local.ChannelEntity
import com.example.ui.components.LiveClock
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.RedAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(UnstableApi::class)
@Composable
fun MultiViewScreen(
    slots: List<ChannelEntity?>,
    activeAudioIndex: Int,
    multiViewMode: Int, // 2 or 4
    allLiveChannels: List<ChannelEntity>,
    onSlotTapped: (Int) -> Unit,
    onSelectChannelForSlot: (Int, ChannelEntity) -> Unit,
    onSetMode: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Channel picker dialog state
    var pickingSlotIndex by remember { mutableStateOf<Int?>(null) }
    var channelSearchQuery by remember { mutableStateOf("") }

    // Lock to landscape for multi-view if desired
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    BackHandler {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onClose()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            onClose()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kapat",
                            tint = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.GridView, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Multi-View (Çoklu İzleme)",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Controls: 2-Channel / 4-Channel mode switcher, Hint, and Live Clock
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Long press hint badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1B2434))
                            .border(1.dp, Color(0xFF2E3E58), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "💡 Basılı Tut: Kanal Seç",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    LiveClock(compact = true, showDate = false)

                    // 2-Kanal Modu Butonu
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                if (multiViewMode == 2) CyanPrimary else DarkCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSetMode(2) }
                            .testTag("multiview_mode_2"),
                        color = if (multiViewMode == 2) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Splitscreen,
                                contentDescription = null,
                                tint = if (multiViewMode == 2) CyanPrimary else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "2 Kanal",
                                color = if (multiViewMode == 2) CyanPrimary else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 4-Kanal Modu Butonu (2x2 Düzen)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                if (multiViewMode == 4) CyanPrimary else DarkCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSetMode(4) }
                            .testTag("multiview_mode_4"),
                        color = if (multiViewMode == 4) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = null,
                                tint = if (multiViewMode == 4) CyanPrimary else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "4 Kanal (2x2)",
                                color = if (multiViewMode == 4) CyanPrimary else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // MULTI-VIEW GRIDS
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                if (multiViewMode == 2) {
                    // 2 Kanal Düzeni (Yan yana)
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MultiViewPane(
                            slotIndex = 0,
                            channel = slots.getOrNull(0),
                            isActiveAudio = (activeAudioIndex == 0),
                            onTap = { onSlotTapped(0) },
                            onChangeChannel = { pickingSlotIndex = 0 },
                            isFourWay = false,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        MultiViewPane(
                            slotIndex = 1,
                            channel = slots.getOrNull(1),
                            isActiveAudio = (activeAudioIndex == 1),
                            onTap = { onSlotTapped(1) },
                            onChangeChannel = { pickingSlotIndex = 1 },
                            isFourWay = false,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    // 4 Kanal Düzeni (2x2 Grid)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MultiViewPane(
                                slotIndex = 0,
                                channel = slots.getOrNull(0),
                                isActiveAudio = (activeAudioIndex == 0),
                                onTap = { onSlotTapped(0) },
                                onChangeChannel = { pickingSlotIndex = 0 },
                                isFourWay = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            MultiViewPane(
                                slotIndex = 1,
                                channel = slots.getOrNull(1),
                                isActiveAudio = (activeAudioIndex == 1),
                                onTap = { onSlotTapped(1) },
                                onChangeChannel = { pickingSlotIndex = 1 },
                                isFourWay = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MultiViewPane(
                                slotIndex = 2,
                                channel = slots.getOrNull(2),
                                isActiveAudio = (activeAudioIndex == 2),
                                onTap = { onSlotTapped(2) },
                                onChangeChannel = { pickingSlotIndex = 2 },
                                isFourWay = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            MultiViewPane(
                                slotIndex = 3,
                                channel = slots.getOrNull(3),
                                isActiveAudio = (activeAudioIndex == 3),
                                onTap = { onSlotTapped(3) },
                                onChangeChannel = { pickingSlotIndex = 3 },
                                isFourWay = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }

        // Channel Selection Dialog for Slot
        if (pickingSlotIndex != null) {
            val targetSlot = pickingSlotIndex!!
            AlertDialog(
                onDismissRequest = { pickingSlotIndex = null },
                containerColor = DarkSurface,
                title = {
                    Text(
                        text = "Slot ${targetSlot + 1} İçin Kanal Seç",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    ) {
                        OutlinedTextField(
                            value = channelSearchQuery,
                            onValueChange = { channelSearchQuery = it },
                            placeholder = { Text("Kanal ara...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val filtered = allLiveChannels.filter {
                            if (channelSearchQuery.isBlank()) true
                            else it.name.contains(channelSearchQuery, ignoreCase = true)
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filtered) { ch ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onSelectChannelForSlot(targetSlot, ch)
                                            pickingSlotIndex = null
                                            channelSearchQuery = ""
                                        },
                                    color = DarkSurfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = ch.name,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = ch.groupTitle,
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { pickingSlotIndex = null }) {
                        Text("İptal", color = TextMuted)
                    }
                }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun MultiViewPane(
    slotIndex: Int,
    channel: ChannelEntity?,
    isActiveAudio: Boolean,
    onTap: () -> Unit,
    onChangeChannel: () -> Unit,
    isFourWay: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(false) }
    var streamError by remember { mutableStateOf<String?>(null) }

    // Each slot creates its own ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            // If 4-channel mode, reduce resolution limit for optimal multi-stream performance
            if (isFourWay) {
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setMaxVideoSize(1280, 720)
                    .build()
            }
        }
    }

    // Set volume: ONLY the active audio slot has sound!
    LaunchedEffect(isActiveAudio) {
        exoPlayer.volume = if (isActiveAudio) 1.0f else 0.0f
    }

    // Load channel stream
    LaunchedEffect(channel?.streamUrl) {
        streamError = null
        if (channel != null && channel.streamUrl.isNotBlank()) {
            isBuffering = true
            try {
                val mediaItem = MediaItem.fromUri(channel.streamUrl)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                streamError = "Yayın açılamadı"
                isBuffering = false
            }
        } else {
            exoPlayer.clearMediaItems()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == Player.STATE_BUFFERING)
                if (state == Player.STATE_READY) streamError = null
            }
            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                streamError = "Hata"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .border(
                width = if (isActiveAudio) 2.5.dp else 1.dp,
                color = if (isActiveAudio) CyanPrimary else DarkCardBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onChangeChannel() }
                )
            }
            .testTag("multiview_slot_$slotIndex")
    ) {
        if (channel != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        isClickable = false
                        isFocusable = false
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )

            // Transparent overlay to reliably capture tap (unmute) and long-press (select channel)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onTap() },
                            onLongPress = { onChangeChannel() }
                        )
                    }
            )
        } else {
            // Empty slot placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F1420)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onChangeChannel,
                        modifier = Modifier
                            .size(44.dp)
                            .background(DarkSurfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Kanal Ekle", tint = CyanPrimary)
                    }
                    Text("Slot ${slotIndex + 1} - Kanal Seç", color = TextMuted, fontSize = 12.sp)
                }
            }
        }

        // Buffering or Error
        if (isBuffering && streamError == null) {
            CircularProgressIndicator(
                color = CyanPrimary,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Center),
                strokeWidth = 3.dp
            )
        }

        if (streamError != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(streamError ?: "", color = RedAccent, fontSize = 11.sp)
            }
        }

        // Top info overlay: Channel title, Audio indicator, Change channel button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = channel?.name ?: "Boş Kanal",
                color = TextPrimary,
                fontSize = if (isFourWay) 11.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Audio status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isActiveAudio) CyanPrimary else Color(0xFF26334D))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(
                            imageVector = if (isActiveAudio) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = null,
                            tint = if (isActiveAudio) Color.Black else TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isActiveAudio) "Ses Açık" else "Sessiz",
                            color = if (isActiveAudio) Color.Black else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Change channel icon button
                IconButton(
                    onClick = onChangeChannel,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChangeCircle,
                        contentDescription = "Kanal Değiştir",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
