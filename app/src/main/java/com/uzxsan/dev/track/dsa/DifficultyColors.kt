package com.uzxsan.dev.track.dsa

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.uzxsan.dev.track.dsa.data.Difficulty
import com.uzxsan.dev.track.ui.theme.EasyDark
import com.uzxsan.dev.track.ui.theme.EasyLight
import com.uzxsan.dev.track.ui.theme.HardDark
import com.uzxsan.dev.track.ui.theme.HardLight
import com.uzxsan.dev.track.ui.theme.MediumDark
import com.uzxsan.dev.track.ui.theme.MediumLight

@Composable
fun difficultyColor(difficulty: Difficulty): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (difficulty) {
        Difficulty.Easy -> if (dark) EasyDark else EasyLight
        Difficulty.Medium -> if (dark) MediumDark else MediumLight
        Difficulty.Hard -> if (dark) HardDark else HardLight
    }
}
