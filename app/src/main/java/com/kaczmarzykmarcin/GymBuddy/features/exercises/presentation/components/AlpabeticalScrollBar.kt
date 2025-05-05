package com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * A scrollbar component that displays alphabetical letters for quick navigation
 */
@Composable
fun AlphabeticalScrollBar(
    letters: List<String>,
    lazyListState: LazyListState,
    letterIndexMap: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    // Użyjmy rememberCoroutineScope zamiast CoroutineScope(Dispatchers.Main)
    // To zapewni, że mamy dostęp do odpowiedniego kontekstu kompozycji z MonotonicFrameClock
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Użyj coroutineScope, który ma dostęp do MonotonicFrameClock
                        letterIndexMap[letter]?.let { index ->
                            coroutineScope.launch {
                                // Animacja przewijania do odpowiedniego indeksu
                                lazyListState.animateScrollToItem(index)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        }
    }
}