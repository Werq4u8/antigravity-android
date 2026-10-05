package com.google.antigravity.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.mobile.ui.theme.*

data class ConversationItem(
    val id: String,
    val title: String,
    val timeAgo: String,
    val isActive: Boolean = false,
    val hasBlueDot: Boolean = false
)

@Composable
fun AntigravitySidebar(
    currentTitle: String,
    onNewConversation: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWorkspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sampleConversations = listOf(
        ConversationItem("1", currentTitle, "1m", isActive = true, hasBlueDot = true),
        ConversationItem("2", "Root Manager and Hiding Modules", "11m", hasBlueDot = true),
        ConversationItem("3", "Android для сборки NetHunter", "3d", hasBlueDot = true),
        ConversationItem("4", "Патчинг Boot.img для ядра", "3d", hasBlueDot = true),
        ConversationItem("5", "Создание APK калькулятора", "7d", hasBlueDot = false),
        ConversationItem("6", "Секундомер с анимацией", "8d", hasBlueDot = false)
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(290.dp)
            .background(SidebarDark)
            .border(width = 1.dp, color = BorderDark.copy(alpha = 0.5f))
            .padding(14.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(AntigravityBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▲",
                    color = AntigravityBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Antigravity",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // "+ New Conversation" Button
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CardDarkVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNewConversation() }
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("+", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Conversation", color = TextPrimary, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Navigation
        SidebarNavItem(icon = Icons.Default.History, title = "Conversation History")
        SidebarNavItem(icon = Icons.Default.Schedule, title = "Scheduled Tasks")
        SidebarNavItem(icon = Icons.Default.Folder, title = "Workspace Files", onClick = onOpenWorkspace)

        Spacer(modifier = Modifier.height(14.dp))

        // Projects Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Projects",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.FilterList, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.Add, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "antigravity-mobile",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Conversations List
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Conversations",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text("+", color = TextMuted, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(sampleConversations) { conv ->
                val bg = if (conv.isActive) CardDarkVariant else Color.Transparent
                val borderMod = if (conv.isActive) Modifier.border(1.dp, BorderDark, RoundedCornerShape(6.dp)) else Modifier

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = bg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(borderMod)
                        .clickable { onSelectConversation(conv.title) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conv.title,
                            color = if (conv.isActive) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (conv.hasBlueDot) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AntigravityBlue)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = conv.timeAgo,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Settings Button
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSettings() }
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = TextSecondary, fontSize = 12.sp)
    }
}
