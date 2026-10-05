package com.google.antigravity.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.mobile.ui.theme.*

@Composable
fun CodeChangePill(
    filesCount: Int,
    additions: Int,
    deletions: Int,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CardDarkVariant,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$filesCount files changed ",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "+$additions ",
                color = GoogleGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "-$deletions",
                color = GoogleRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = CardDark,
                modifier = Modifier
                    .clickable { onReviewClick() }
                    .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Review",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun MessageReactions(
    onCopy: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Outlined.ThumbUp, contentDescription = "Like", tint = TextMuted, modifier = Modifier.size(13.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Outlined.ThumbDown, contentDescription = "Dislike", tint = TextMuted, modifier = Modifier.size(13.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
fun CodeBlockSnippet(
    code: String,
    language: String = "bash",
    onCopy: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF16181D),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
    ) {
        Column {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111216))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy code",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(12.dp)
                        .clickable { onCopy() }
                )
            }
            Text(
                text = code,
                color = Color(0xFFE5E7EB),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}
