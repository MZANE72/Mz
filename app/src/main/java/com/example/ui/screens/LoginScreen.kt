package com.example.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LiveClock
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun LoginScreen(
    isLoading: Boolean,
    loadingMessage: String,
    errorMessage: String?,
    onClearError: () -> Unit,
    onLoadUrl: (url: String, expirationMillis: Long, name: String) -> Unit,
    onLoadFile: (uri: Uri, expirationMillis: Long) -> Unit,
    onLoginXtream: (server: String, user: String, pass: String, expirationMillis: Long) -> Unit,
    onLoadDemoStreams: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: URL, 1: File, 2: Xtream

    // URL Tab state
    var urlInput by remember { mutableStateOf("") }
    var playlistNameInput by remember { mutableStateOf("") }
    var urlExpirationMillis by remember { mutableLongStateOf(0L) }

    // File Tab state
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var fileExpirationMillis by remember { mutableLongStateOf(0L) }

    // Xtream Tab state
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }
    var xtreamExpirationMillis by remember { mutableLongStateOf(0L) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = uri.lastPathSegment ?: "Seçilen Playlist Dosyası"
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR")) }

    fun showDatePicker(initialMillis: Long, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        if (initialMillis > 0L) {
            calendar.timeInMillis = initialMillis
        } else {
            calendar.add(Calendar.MONTH, 1) // default 1 month later
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth, 23, 59, 59)
                onDateSelected(selectedCal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Live Clock and Brand
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(CyanPrimary, VioletSecondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "NOVA IPTV",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Gelişmiş Oynatıcı",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Saat, Dakika, Saniye ve Tarih
                LiveClock(compact = true, showDate = true)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // YASAL UYARI BANNERI (ZORUNLU)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1B1B26))
                    .border(1.dp, AmberAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Yasal Uyarı",
                            color = AmberAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Yalnızca yasal hakka sahip olduğunuz playlist ve hesapları ekleyin. Uygulama herhangi bir telifli içerik veya kanal sağlamaz.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Error banner
            AnimatedVisibility(visible = errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 700.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RedAccent.copy(alpha = 0.15f))
                        .border(1.dp, RedAccent, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = RedAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = errorMessage ?: "",
                            color = RedAccent,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearError, modifier = Modifier.size(24.dp)) {
                            Text("✕", color = RedAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Tabs Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    // Tabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = CyanPrimary,
                        edgePadding = 0.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = CyanPrimary,
                                height = 3.dp
                            )
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("URL ile Ekle", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                                }
                            },
                            selectedContentColor = CyanPrimary,
                            unselectedContentColor = TextMuted
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Dosyadan Ekle", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                                }
                            },
                            selectedContentColor = CyanPrimary,
                            unselectedContentColor = TextMuted
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Hesap ile Giriş", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                                }
                            },
                            selectedContentColor = CyanPrimary,
                            unselectedContentColor = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Tab Contents
                    when (selectedTab) {
                        0 -> {
                            // 1) URL ile Ekle
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                OutlinedTextField(
                                    value = urlInput,
                                    onValueChange = { urlInput = it },
                                    label = { Text("M3U / M3U8 Playlist URL") },
                                    placeholder = { Text("http://example.com/playlist.m3u8") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("playlist_url_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyanPrimary,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = DarkSurfaceVariant,
                                        unfocusedContainerColor = DarkSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = playlistNameInput,
                                    onValueChange = { playlistNameInput = it },
                                    label = { Text("Playlist Başlığı (İsteğe Bağlı)") },
                                    placeholder = { Text("Örn: Kişisel Yayın Listem") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyanPrimary,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = DarkSurfaceVariant,
                                        unfocusedContainerColor = DarkSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Expiration Date Picker
                                ExpirationPickerRow(
                                    expirationMillis = urlExpirationMillis,
                                    dateFormatter = dateFormatter,
                                    onPickDate = {
                                        showDatePicker(urlExpirationMillis) { picked ->
                                            urlExpirationMillis = picked
                                        }
                                    },
                                    onClearDate = { urlExpirationMillis = 0L }
                                )

                                Button(
                                    onClick = {
                                        onLoadUrl(urlInput, urlExpirationMillis, playlistNameInput)
                                    },
                                    enabled = !isLoading && urlInput.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("load_url_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanPrimary,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Playlisti Yükle ve Başlat", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }

                        1 -> {
                            // 2) Dosyadan Ekle
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceVariant)
                                        .border(1.dp, if (selectedFileUri != null) CyanPrimary else DarkCardBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            filePickerLauncher.launch(
                                                arrayOf(
                                                    "audio/x-mpegurl",
                                                    "application/x-mpegurl",
                                                    "application/vnd.apple.mpegurl",
                                                    "text/plain",
                                                    "*/*"
                                                )
                                            )
                                        }
                                        .padding(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (selectedFileUri != null) Icons.Default.CheckCircle else Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = if (selectedFileUri != null) GreenSuccess else CyanPrimary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = selectedFileName ?: ".m3u, .m3u8 veya .txt Dosyası Seçin",
                                            color = if (selectedFileUri != null) TextPrimary else TextSecondary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Telefondan yerel playlist seçmek için dokunun",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Expiration Date Picker
                                ExpirationPickerRow(
                                    expirationMillis = fileExpirationMillis,
                                    dateFormatter = dateFormatter,
                                    onPickDate = {
                                        showDatePicker(fileExpirationMillis) { picked ->
                                            fileExpirationMillis = picked
                                        }
                                    },
                                    onClearDate = { fileExpirationMillis = 0L }
                                )

                                Button(
                                    onClick = {
                                        selectedFileUri?.let { onLoadFile(it, fileExpirationMillis) }
                                    },
                                    enabled = !isLoading && selectedFileUri != null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("load_file_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanPrimary,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Dosyadaki Kanalları Yükle", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }

                        2 -> {
                            // 3) Hesap ile Giriş (Xtream Codes API)
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                OutlinedTextField(
                                    value = xtreamServer,
                                    onValueChange = { xtreamServer = it },
                                    label = { Text("Sunucu Adresi (URL:Port)") },
                                    placeholder = { Text("http://sunucu.com:8080") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("xtream_server_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyanPrimary,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = DarkSurfaceVariant,
                                        unfocusedContainerColor = DarkSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = xtreamUser,
                                    onValueChange = { xtreamUser = it },
                                    label = { Text("Kullanıcı Adı") },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("xtream_user_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyanPrimary,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = DarkSurfaceVariant,
                                        unfocusedContainerColor = DarkSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = xtreamPass,
                                    onValueChange = { xtreamPass = it },
                                    label = { Text("Şifre") },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = TextMuted
                                            )
                                        }
                                    },
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("xtream_pass_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyanPrimary,
                                        unfocusedBorderColor = DarkCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = DarkSurfaceVariant,
                                        unfocusedContainerColor = DarkSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Expiration note for Xtream
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF141D2D), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "Abonelik bitiş tarihi Xtream sunucusundan otomatik olarak algılanacaktır.",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        onLoginXtream(xtreamServer, xtreamUser, xtreamPass, xtreamExpirationMillis)
                                    },
                                    enabled = !isLoading && xtreamServer.isNotBlank() && xtreamUser.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("xtream_login_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanPrimary,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(Icons.Default.Tv, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Xtream ile Bağlan ve Yükle", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Demo / Test streams button
            OutlinedButton(
                onClick = onLoadDemoStreams,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .height(48.dp)
                    .testTag("demo_streams_button"),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(CyanPrimary.copy(alpha = 0.6f), VioletSecondary.copy(alpha = 0.6f)))
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Yasal Örnek Yayınlar ile Dene (NASA TV, Açık Test Akışları)",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Fullscreen Loading Overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .background(DarkSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, CyanPrimary, RoundedCornerShape(16.dp))
                        .padding(28.dp)
                ) {
                    CircularProgressIndicator(
                        color = CyanPrimary,
                        modifier = Modifier.size(46.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = loadingMessage.ifBlank { "İşlem yapılıyor..." },
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpirationPickerRow(
    expirationMillis: Long,
    dateFormatter: SimpleDateFormat,
    onPickDate: () -> Unit,
    onClearDate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onPickDate)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "Abonelik Bitiş Tarihi",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = if (expirationMillis > 0L) {
                        dateFormatter.format(Date(expirationMillis))
                    } else {
                        "Süresiz / Tarih Seçilmedi"
                    },
                    color = if (expirationMillis > 0L) AmberAccent else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (expirationMillis > 0L) {
            Text(
                text = "Temizle",
                color = RedAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onClearDate)
                    .padding(4.dp)
            )
        } else {
            Text(
                text = "Seç",
                color = CyanPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
