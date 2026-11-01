package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChannelEntity
import com.example.ui.components.ChannelItem
import com.example.ui.components.LiveClock
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun LiveTvScreen(
    channels: List<ChannelEntity>,
    allChannels: List<ChannelEntity>,
    recentChannels: List<ChannelEntity>,
    selectedCategory: String,
    searchQuery: String,
    onSelectCategory: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onDeleteChannel: (ChannelEntity) -> Unit,
    onRenameChannel: (channelId: String, newName: String) -> Unit,
    onMoveUp: (ChannelEntity) -> Unit,
    onMoveDown: (ChannelEntity) -> Unit,
    onRestoreDeleted: () -> Unit,
    onOpenMultiView: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Extract unique categories
    val categories = remember(allChannels) {
        val set = linkedSetOf("Tümü", "Favoriler")
        allChannels.map { it.groupTitle.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { set.add(it) }
        set.toList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Top Action Row: Search, Clock, and Multi-View Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Kanal veya kategori ara...") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("channel_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Multi-View Button
            Button(
                onClick = onOpenMultiView,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("open_multiview_button")
            ) {
                Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Multi-View", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = (cat == selectedCategory)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelectCategory(cat) }
                        .testTag("category_chip_$cat"),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (cat == "Favoriler") {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = cat,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Son İzlenen Kanallar Row (Recent Channels)
        if (recentChannels.isNotEmpty() && searchQuery.isBlank() && selectedCategory == "Tümü") {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                Text(
                    text = "Son İzlenenler",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recentChannels) { recentCh ->
                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .clickable { onPlayChannel(recentCh) }
                            .testTag("recent_channel_${recentCh.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = recentCh.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = recentCh.groupTitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Channel Count Summary & Restore option
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${channels.size} Kanal Listeleniyor",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onRestoreDeleted)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Text("Silinenleri Geri Al", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Channels List
        if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (selectedCategory == "Favoriler") "Henüz favori kanal eklenmedi" else "Gösterilecek kanal bulunamadı",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (selectedCategory == "Favoriler") "Kanalların yanındaki yıldız simgesine dokunarak favorilerinize ekleyebilirsiniz." else "Arama filtrenizi temizlemeyi deneyin.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(channels, key = { _, item -> item.id }) { index, ch ->
                    ChannelItem(
                        channel = ch,
                        onClick = { onPlayChannel(ch) },
                        onToggleFavorite = { onToggleFavorite(ch) },
                        onDelete = { onDeleteChannel(ch) },
                        onRename = { newName -> onRenameChannel(ch.id, newName) },
                        onMoveUp = { onMoveUp(ch) },
                        onMoveDown = { onMoveDown(ch) },
                        canMoveUp = index > 0,
                        canMoveDown = index < channels.size - 1
                    )
                }
            }
        }
    }
}
