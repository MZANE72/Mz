package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.HourglassDisabled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlaylistConfig
import com.example.ui.components.LiveClock
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.RedAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpiredScreen(
    config: PlaylistConfig,
    onAddNewPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr", "TR")) }
    val expiredDateString = remember(config.expirationMillis) {
        if (config.expirationMillis > 0L) {
            dateFormatter.format(Date(config.expirationMillis))
        } else {
            "Belirtilen süre doldu"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 550.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .border(1.dp, RedAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LiveClock(compact = true, showDate = true)

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(RedAccent.copy(alpha = 0.15f))
                    .border(2.dp, RedAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassDisabled,
                    contentDescription = null,
                    tint = RedAccent,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = "Abonelik Süresi Dolmuş",
                color = RedAccent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Bu playlist veya hesaba tanımlanan abonelik kullanım süresi sona ermiştir.\n\nBitiş Zamanı: $expiredDateString",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1B26), RoundedCornerShape(10.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Mevcut Kaynak: ${config.name.ifBlank { config.sourceUri }}",
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "İzlemeye devam etmek için lütfen geçerli ve yeni bir M3U playlisti veya Xtream hesabı ekleyin.",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onAddNewPlaylist,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("expired_add_new_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.AddLink, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Yeni URL / Playlist Ekle", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
