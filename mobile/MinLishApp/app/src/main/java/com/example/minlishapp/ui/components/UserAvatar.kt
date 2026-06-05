package com.example.minlishapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AvatarTier(
    val title: String,
    val titleVi: String,
    val colors: List<Color>,
    val textColor: Color
) {
    BRONZE(
        "Bronze", 
        "Đồng",
        listOf(Color(0xFFCD7F32), Color(0xFF8B4513)),
        Color(0xFFCD7F32)
    ),
    SILVER(
        "Silver", 
        "Bạc",
        listOf(Color(0xFFE0E0E0), Color(0xFF9E9E9E)),
        Color(0xFF9E9E9E)
    ),
    GOLD(
        "Gold", 
        "Vàng",
        listOf(Color(0xFFFFDF00), Color(0xFFD4AF37)),
        Color(0xFFD4AF37)
    ),
    DIAMOND(
        "Diamond", 
        "Kim Cương",
        listOf(Color(0xFF00FFFF), Color(0xFF00BFFF)),
        Color(0xFF00BFFF)
    )
}

fun getTierForLevel(level: Int): AvatarTier {
    return when {
        level <= 5 -> AvatarTier.BRONZE
        level <= 15 -> AvatarTier.SILVER
        level <= 30 -> AvatarTier.GOLD
        else -> AvatarTier.DIAMOND
    }
}

@Composable
fun UserAvatar(
    name: String,
    level: Int,
    size: Dp = 80.dp,
    showLevelBadge: Boolean = true
) {
    val tier = getTierForLevel(level)
    val initial = if (name.isNotEmpty()) name.first().toString().uppercase() else "L"
    val fontSize = (size.value * 0.4).sp
    val badgeSize = (size.value * 0.35).dp
    val badgeFontSize = (size.value * 0.15).sp

    Box(contentAlignment = Alignment.Center) {
        // Main Avatar Circle
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .border(
                    width = maxOf(2.dp, size * 0.04f),
                    brush = Brush.linearGradient(colors = tier.colors),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Black
            )
        }

        // Level Badge
        if (showLevelBadge) {
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .align(Alignment.BottomEnd)
                    .offset(x = badgeSize * 0.1f, y = badgeSize * 0.1f)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors = tier.colors))
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level.toString(),
                    color = Color.White,
                    fontSize = badgeFontSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
