package com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.data.model.Exercise
import com.kaczmarzykmarcin.GymBuddy.ui.theme.AppBackgroundLight
import com.kaczmarzykmarcin.GymBuddy.ui.theme.DarkGray
import com.kaczmarzykmarcin.GymBuddy.ui.theme.LightGrayBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExerciseDetailBottomSheet(
    exercise: Exercise,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()
    var isBookmarked by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Używamy danych z bazy danych
    val exerciseImages = remember(exercise) {
        when {
            exercise.images.isNotEmpty() -> exercise.images
            exercise.imageUrl != null -> listOf(exercise.imageUrl)
            else -> emptyList()
        }
    }

    // Pager state dla karuzeli zdjęć
    val pagerState = rememberPagerState(pageCount = { exerciseImages.size.coerceAtLeast(1) })

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        containerColor = AppBackgroundLight,
        modifier = Modifier.padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues())
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header z przyciskami
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
                Box(
                    modifier = Modifier.width(48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color.Black
                        )
                    }
                }

                // Pusty środek
                Spacer(modifier = Modifier.weight(1f))

                // Bookmark button
                Box(
                    modifier = Modifier.width(48.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = { isBookmarked = !isBookmarked }
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = stringResource(R.string.bookmark),
                            tint = if (isBookmarked) Color.Blue else Color.Black
                        )
                    }
                }
            }

            // Main content - scrollable
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    // Karuzela zdjęć ćwiczenia
                    if (exerciseImages.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            // Horizontal Pager dla zdjęć
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                            ) { page ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(exerciseImages[page])
                                        .crossfade(true)
                                        .error(android.R.drawable.ic_menu_gallery) // Fallback image
                                        .build(),
                                    contentDescription = exercise.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(LightGrayBackground),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Wskaźniki kropek (tylko jeśli jest więcej niż jedno zdjęcie)
                            if (exerciseImages.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    repeat(exerciseImages.size) { iteration ->
                                        val color = if (pagerState.currentPage == iteration) {
                                            Color.Blue
                                        } else {
                                            Color.LightGray
                                        }

                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Placeholder jeśli brak zdjęć
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LightGrayBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_image_available),
                                color = DarkGray
                            )
                        }
                    }
                }

                item {
                    // Nazwa ćwiczenia
                    Text(
                        text = exercise.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Podstawowe informacje
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        exercise.level?.let { level ->
                            Text(
                                text = level.capitalizeFirstLetter(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkGray,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }

                    }
                }

                // Sekcja szczegółowych informacji
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Typ ruchu
                        exercise.mechanic?.let { mechanic ->
                            ExerciseInfoCard(
                                title = stringResource(R.string.movement_type),
                                value = mechanic.capitalizeFirstLetter(),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Siła
                        exercise.force?.let { force ->
                            ExerciseInfoCard(
                                title = stringResource(R.string.force_type),
                                value = force.capitalizeFirstLetter(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sprzęt
                        exercise.equipment?.let { equipment ->
                            ExerciseInfoCard(
                                title = stringResource(R.string.equipment),
                                value = equipment.capitalizeFirstLetter(),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Kategoria
                        ExerciseInfoCard(
                            title = stringResource(R.string.category),
                            value = exercise.category.capitalizeFirstLetter(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Główne mięśnie
                if (exercise.primaryMuscles.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.primary_muscles),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            exercise.primaryMuscles.forEach { muscle ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.Blue.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = muscle.capitalizeFirstLetter(),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = Color.Blue,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // Wspomagające mięśnie

                if (exercise.secondaryMuscles.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.secondary_muscles),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(exercise.secondaryMuscles.size) { index ->
                                val muscle = exercise.secondaryMuscles[index]
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = LightGrayBackground
                                ) {
                                    Text(
                                        text = muscle.capitalizeFirstLetter(),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = Color.Black,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // Instrukcje
                if (exercise.instructions.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.instructions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    items(exercise.instructions.size) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black,
                                modifier = Modifier.padding(top = 2.dp, end = 12.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = exercise.instructions[index],
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Notatki
                if (exercise.notes.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.notes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        exercise.notes.forEach { note ->
                            Text(
                                text = "• $note",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkGray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                // Wskazówki
                if (exercise.tips.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.tips),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        exercise.tips.forEach { tip ->
                            Text(
                                text = "💡 $tip",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkGray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                // Extra space at bottom
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ExerciseInfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(width = 1.dp, color = Color(0x1A000000), shape = RoundedCornerShape(24.dp)),

        colors = CardDefaults.cardColors(containerColor = Color.White),



    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = DarkGray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun String.capitalizeFirstLetter(): String {
    return if (this.isEmpty()) this else this.substring(0, 1).uppercase() + this.substring(1)
}