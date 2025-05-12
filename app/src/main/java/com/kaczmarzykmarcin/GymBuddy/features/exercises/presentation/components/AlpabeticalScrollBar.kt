package com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components

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
import androidx.compose.runtime.remember
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
 *
 * @param letters Lista liter do wyświetlenia w pasku
 * @param lazyListState Stan listy, aby przewijać do odpowiedniej pozycji
 * @param letterIndexMap Mapa litera -> pozycja w liście
 * @param modifier Dodatkowy modyfikator do dostosowania wyglądu
 * @param compact Czy wyświetlać kompaktową wersję scrollbara (mniejsze odstępy, mniejsza czcionka)
 */
@Composable
fun AlphabeticalScrollBar(
    letters: List<String>,
    lazyListState: LazyListState,
    letterIndexMap: Map<String, Int>,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    // Użyjemy rememberCoroutineScope zamiast CoroutineScope(Dispatchers.Main)
    val coroutineScope = rememberCoroutineScope()

    // Dostosowujemy rozmiar elementów i czcionki na podstawie parametru compact
    val itemSize = if (compact) 16.dp else 20.dp
    val fontSize = if (compact) 10.sp else 12.sp
    val verticalPadding = if (compact) 0.5.dp else 2.dp

    Column(
        modifier = modifier
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            Box(
                modifier = Modifier
                    .size(itemSize)
                    .padding(vertical = verticalPadding)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
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
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        }
    }
}