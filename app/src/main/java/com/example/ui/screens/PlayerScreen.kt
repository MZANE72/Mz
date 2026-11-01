package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.local.ChannelEntity
import com.example.ui.components.LiveClock
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.RedAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale

data class SubtitleTrackInfo(
    val trackGroup: TrackGroup,
    val trackIndex: Int,
    val language: String,
    val label: String,
    val isSelected: Boolean
)

data class AudioTrackInfo(
    val trackGroup: TrackGroup,
    val trackIndex: Int,
    val language: String,
    val label: String,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: ChannelEntity,
    onClose: () -> Unit,
    onNextChannel: (() -> Unit)? = null,
    onPrevChannel: (() -> Unit)? = null,
    onRecallChannel: (() -> Unit)? = null,
    onSaveResumePosition: ((channelId: String, positionMs: Long, durationMs: Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isControlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var isMuted by remember { mutableStateOf(false) }

    // Resize mode (0: Fit, 3: Zoom, 4: Fill)
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    val resizeModes = remember {
        listOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT to "Sığdır (Fit)",
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Yakınlaştır (Zoom)",
            AspectRatioFrameLayout.RESIZE_MODE_FILL to "Doldur (16:9)"
        )
    }

    // Orientation state: 1: Landscape, 2: Portrait
    var currentOrientationMode by remember { mutableIntStateOf(1) } // Default landscape

    // Progress and duration for Movies/Series
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    val isVod = channel.type == ChannelEntity.TYPE_MOVIE || channel.type == ChannelEntity.TYPE_SERIES

    // Resume position prompt
    var showResumePrompt by remember {
        mutableStateOf(isVod && channel.resumePositionMs > 15000L && channel.resumePositionMs < (channel.durationMs - 20000L))
    }

    // Subtitle & Audio tracks dialogs
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var availableSubtitles by remember { mutableStateOf<List<SubtitleTrackInfo>>(emptyList()) }

    var showAudioDialog by remember { mutableStateOf(false) }
    var availableAudioTracks by remember { mutableStateOf<List<AudioTrackInfo>>(emptyList()) }

    // Playback speed
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Setup ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Set orientation
    LaunchedEffect(currentOrientationMode) {
        activity?.requestedOrientation = when (currentOrientationMode) {
            1 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            2 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_USER
        }
    }

    // Back handler resets orientation to unspecified
    BackHandler {
        onSaveResumePosition?.invoke(channel.id, currentPositionMs, durationMs)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onClose()
    }

    // Load stream URL
    LaunchedEffect(channel.streamUrl) {
        playerError = null
        isBuffering = true
        try {
            val mediaItem = MediaItem.fromUri(channel.streamUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            playerError = "Yayın açılamadı: ${e.localizedMessage}"
            isBuffering = false
        }
    }

    // Player event listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    playerError = null
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playerError = "Yayın akışı kesildi: ${error.message ?: "Bağlantı hatası"}"
            }

            override fun onTracksChanged(tracks: Tracks) {
                // Extract subtitle tracks
                val subs = mutableListOf<SubtitleTrackInfo>()
                val audios = mutableListOf<AudioTrackInfo>()

                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_TEXT) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val lang = format.language ?: "Bilinmiyor"
                            val label = format.label ?: "Alt Yazı ${subs.size + 1} ($lang)"
                            subs.add(
                                SubtitleTrackInfo(
                                    trackGroup = group.mediaTrackGroup,
                                    trackIndex = i,
                                    language = lang,
                                    label = label,
                                    isSelected = group.isTrackSelected(i)
                                )
                            )
                        }
                    } else if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val lang = format.language ?: "Varsayılan"
                            val label = format.label ?: "Ses Parçası ${audios.size + 1} ($lang)"
                            audios.add(
                                AudioTrackInfo(
                                    trackGroup = group.mediaTrackGroup,
                                    trackIndex = i,
                                    language = lang,
                                    label = label,
                                    isSelected = group.isTrackSelected(i)
                                )
                            )
                        }
                    }
                }
                availableSubtitles = subs
                availableAudioTracks = audios
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            onSaveResumePosition?.invoke(channel.id, currentPositionMs, durationMs)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Progress update loop for VOD (Movies & Series)
    LaunchedEffect(isPlaying, isVod) {
        while (isVod) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(1000L)
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(6000L)
            isControlsVisible = false
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val hr = totalSec / 3600
        val min = (totalSec % 3600) / 60
        val sec = totalSec % 60
        return if (hr > 0) {
            String.format(Locale.ROOT, "%02d:%02d:%02d", hr, min, sec)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", min, sec)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isControlsVisible = !isControlsVisible
            }
            .focusable()
    ) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = resizeModes[resizeModeIndex].first
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = resizeModes[resizeModeIndex].first
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering && playerError == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CyanPrimary,
                    modifier = Modifier.size(54.dp),
                    strokeWidth = 4.dp
                )
            }
        }

        // Error message overlay
        if (playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .background(DarkSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, RedAccent, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Yayın Hatası",
                        color = RedAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = playerError ?: "",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(
                            onClick = {
                                playerError = null
                                exoPlayer.prepare()
                                exoPlayer.play()
                            }
                        ) {
                            Text("Tekrar Dene", color = CyanPrimary, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = {
                            onSaveResumePosition?.invoke(channel.id, currentPositionMs, durationMs)
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            onClose()
                        }) {
                            Text("Geri Dön", color = TextMuted)
                        }
                    }
                }
            }
        }

        // Resume Playback Prompt Modal
        if (showResumePrompt) {
            AlertDialog(
                onDismissRequest = { showResumePrompt = false },
                containerColor = DarkSurface,
                title = { Text("Kaldığınız Yerden Devam Edin", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "Bu içeriği daha önce ${formatDuration(channel.resumePositionMs)} dakikasına kadar izlediniz. Kaldığınız yerden devam etmek ister misiniz?",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            exoPlayer.seekTo(channel.resumePositionMs)
                            currentPositionMs = channel.resumePositionMs
                            showResumePrompt = false
                        }
                    ) {
                        Text("Devam Et (${formatDuration(channel.resumePositionMs)})")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResumePrompt = false }) {
                        Text("Baştan Başla", color = TextMuted)
                    }
                }
            )
        }

        // CONTROLS OVERLAY
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                // TOP BAR: Back, Channel info, Live Clock, Aspect Ratio, Orientation, PiP, External Player
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(
                            onClick = {
                                onSaveResumePosition?.invoke(channel.id, currentPositionMs, durationMs)
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                onClose()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = Color.White
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (channel.channelNumber > 0) {
                                    Text(
                                        text = "#${channel.channelNumber} ",
                                        color = CyanPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = channel.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${channel.groupTitle} • ${if (isVod) "VOD İçerik" else "Canlı Yayın"}",
                                color = CyanPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Top Right Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Live Clock with seconds & date
                        LiveClock(compact = true, showDate = true)

                        // External Player Launcher (VLC / MX Player)
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(channel.streamUrl), "video/*")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "Harici Oynatıcı Seçin"))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .testTag("external_player_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Harici Oynatıcı",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Picture in Picture (PiP)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(
                                onClick = {
                                    try {
                                        val pipParams = PictureInPictureParams.Builder().build()
                                        activity?.enterPictureInPictureMode(pipParams)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .testTag("pip_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Resim İçinde Resim (PiP)",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Aspect Ratio Toggle
                        IconButton(
                            onClick = {
                                resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Ekran Oranı",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Orientation Switcher (Yatay / Dikey)
                        IconButton(
                            onClick = {
                                currentOrientationMode = when (currentOrientationMode) {
                                    1 -> 2
                                    2 -> 1
                                    else -> 1
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .testTag("orientation_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Ekran Döndür",
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // CENTER CONTROLS: Zapping / 10s-30s Rewind, Play/Pause, Forward
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isVod) {
                        // 30s Rewind
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition - 30000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .testTag("rewind_30s_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay30,
                                contentDescription = "30s Geri",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // 10s Rewind
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .testTag("rewind_10s_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "10s Geri",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    } else if (onPrevChannel != null) {
                        // Channel Down Zapping
                        IconButton(
                            onClick = { onPrevChannel() },
                            modifier = Modifier
                                .size(54.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .testTag("channel_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Önceki Kanal",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Main Play / Pause
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(CyanPrimary, CircleShape)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                            tint = Color.Black,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    if (isVod) {
                        // 10s Forward
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .testTag("forward_10s_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "10s İleri",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        // 30s Forward
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition + 30000L).coerceAtMost(durationMs)
                                exoPlayer.seekTo(newPos)
                                currentPositionMs = newPos
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .testTag("forward_30s_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward30,
                                contentDescription = "30s İleri",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else if (onNextChannel != null) {
                        // Channel Up Zapping
                        IconButton(
                            onClick = { onNextChannel() },
                            modifier = Modifier
                                .size(54.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .testTag("channel_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Sonraki Kanal",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // BOTTOM CONTROLS: Scrubber bar, Recall, Audio track, Subtitle, Speed
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    if (isVod && durationMs > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = formatDuration(currentPositionMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Slider(
                                value = currentPositionMs.toFloat(),
                                onValueChange = { currentPositionMs = it.toLong() },
                                onValueChangeFinished = {
                                    exoPlayer.seekTo(currentPositionMs)
                                },
                                valueRange = 0f..durationMs.toFloat(),
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanPrimary,
                                    activeTrackColor = CyanPrimary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )

                            Text(
                                text = formatDuration(durationMs),
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Bottom info bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Mute toggle
                            IconButton(
                                onClick = {
                                    isMuted = !isMuted
                                    exoPlayer.volume = if (isMuted) 0f else 1f
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Ses",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Recall (Son Kanal) in live TV mode
                            if (!isVod && onRecallChannel != null) {
                                IconButton(
                                    onClick = { onRecallChannel() },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .testTag("recall_channel_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "Son Kanal",
                                            tint = CyanPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text("Son Kanal", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Right actions: Speed, Audio Track, Subtitle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Playback Speed (Hız)
                            if (isVod) {
                                IconButton(
                                    onClick = { showSpeedDialog = true },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .testTag("speed_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(Icons.Default.Speed, contentDescription = "Hız", tint = Color.White, modifier = Modifier.size(16.dp))
                                        Text("${currentSpeed}x", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Audio Track Selector (Ses Parçası)
                            IconButton(
                                onClick = { showAudioDialog = true },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .testTag("audio_track_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(Icons.Default.Audiotrack, contentDescription = "Ses Kanalı", tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text("Ses", color = Color.White, fontSize = 11.sp)
                                }
                            }

                            // Subtitles (Alt Yazı)
                            IconButton(
                                onClick = { showSubtitleDialog = true },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .testTag("subtitle_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ClosedCaption,
                                        contentDescription = "Alt Yazı",
                                        tint = if (availableSubtitles.any { it.isSelected }) CyanPrimary else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Alt Yazı",
                                        color = if (availableSubtitles.any { it.isSelected }) CyanPrimary else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Playback Speed Dialog
        if (showSpeedDialog) {
            val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                containerColor = DarkSurface,
                title = { Text("Oynatma Hızı", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        speedOptions.forEach { speed ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        currentSpeed = speed
                                        exoPlayer.setPlaybackSpeed(speed)
                                        showSpeedDialog = false
                                    },
                                color = if (currentSpeed == speed) CyanPrimary.copy(alpha = 0.15f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${speed}x ${if (speed == 1.0f) "(Normal)" else ""}", color = if (currentSpeed == speed) CyanPrimary else TextPrimary)
                                    if (currentSpeed == speed) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = CyanPrimary)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Kapat", color = CyanPrimary)
                    }
                }
            )
        }

        // Audio Track Selection Dialog
        if (showAudioDialog) {
            AlertDialog(
                onDismissRequest = { showAudioDialog = false },
                containerColor = DarkSurface,
                title = { Text("Ses Kanalı Seçimi", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (availableAudioTracks.isEmpty()) {
                            Text("Bu yayında tek bir varsayılan ses kanalı bulunmaktadır.", color = TextMuted, fontSize = 13.sp)
                        } else {
                            availableAudioTracks.forEach { audio ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                .buildUpon()
                                                .setOverrideForType(
                                                    TrackSelectionOverride(audio.trackGroup, audio.trackIndex)
                                                )
                                                .build()
                                            showAudioDialog = false
                                        },
                                    color = if (audio.isSelected) CyanPrimary.copy(alpha = 0.15f) else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${audio.label} (${audio.language})", color = if (audio.isSelected) CyanPrimary else TextPrimary)
                                        if (audio.isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = CyanPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAudioDialog = false }) {
                        Text("Kapat", color = CyanPrimary)
                    }
                }
            )
        }

        // Subtitle Selection Modal Dialog
        if (showSubtitleDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                containerColor = DarkSurface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ClosedCaption, contentDescription = null, tint = CyanPrimary)
                        Text("Alt Yazı Seçimi", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isNoneSelected = availableSubtitles.none { it.isSelected }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                        .build()
                                    showSubtitleDialog = false
                                },
                            color = if (isNoneSelected) CyanPrimary.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Kapalı", color = if (isNoneSelected) CyanPrimary else TextPrimary)
                                if (isNoneSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = CyanPrimary)
                                }
                            }
                        }

                        if (availableSubtitles.isEmpty()) {
                            Text(
                                text = "Bu yayında gömülü alt yazı kanalı bulunamadı.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            availableSubtitles.forEach { sub ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                .buildUpon()
                                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                                .setOverrideForType(
                                                    TrackSelectionOverride(sub.trackGroup, sub.trackIndex)
                                                )
                                                .build()
                                            showSubtitleDialog = false
                                        },
                                    color = if (sub.isSelected) CyanPrimary.copy(alpha = 0.15f) else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${sub.label} (${sub.language})",
                                            color = if (sub.isSelected) CyanPrimary else TextPrimary
                                        )
                                        if (sub.isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = CyanPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSubtitleDialog = false }) {
                        Text("Kapat", color = CyanPrimary)
                    }
                }
            )
        }
    }
}
