package com.google.antigravity.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.antigravity.mobile.ui.theme.*

@Composable
fun AntigravityInputDock(
    inputText: String,
    onInputChange: (String) -> Unit,
    selectedModel: String,
    onSelectModel: (String) -> Unit,
    onSend: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var showModelMenu by remember { mutableStateOf(false) }
    val models = listOf(
        "Gemini 2.5 Flash",
        "Gemini 2.5 Pro",
        "Gemini 1.5 Flash",
        "Gemini 1.5 Pro"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardDark,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Text Input Field
            TextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        "Ask Antigravity anything...",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = 120.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                enabled = isEnabled
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom action dock inside input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "+" icon button
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {}
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Model Selector Pill (like in screenshot: "+ Gemini 3.8 Flash (Medium) ^")
                Box {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardDarkVariant,
                        modifier = Modifier
                            .clickable { showModelMenu = true }
                            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedModel,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "▾",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false },
                        modifier = Modifier.background(CardDarkVariant)
                    ) {
                        models.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m, color = TextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    onSelectModel(m)
                                    showModelMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Mic icon
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Circular Google Blue Send Button (like in screenshot)
                val canSend = isEnabled && inputText.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (canSend) AntigravityBlue else CardDarkVariant)
                        .clickable(enabled = canSend) { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Send",
                        tint = if (canSend) Color.White else TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
