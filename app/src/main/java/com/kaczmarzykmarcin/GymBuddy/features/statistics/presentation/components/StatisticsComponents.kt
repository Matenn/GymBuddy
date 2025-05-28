package com.kaczmarzykmarcin.GymBuddy.features.statistics.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.data.model.Exercise
import com.kaczmarzykmarcin.GymBuddy.data.model.WorkoutCategory
import com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components.ExerciseSelectionBottomSheet
import com.kaczmarzykmarcin.GymBuddy.features.statistics.data.model.StatType
import com.kaczmarzykmarcin.GymBuddy.features.statistics.data.model.TimePeriod
import com.kaczmarzykmarcin.GymBuddy.ui.theme.LightGrayBackground
import kotlinx.coroutines.launch

@Composable
fun TimePeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll when selection changes
    LaunchedEffect(selectedPeriod) {
        val selectedIndex = TimePeriod.values().indexOf(selectedPeriod)
        if (selectedIndex != -1) {
            coroutineScope.launch {
                listState.animateScrollToItem(selectedIndex)
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp, 0.dp, 8.dp, 0.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(TimePeriod.values()) { period ->
                val isSelected = period == selectedPeriod

                Button(
                    onClick = { onPeriodSelected(period) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color.Black else Color.Transparent,
                        contentColor = if (isSelected) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(32.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = period.displayName,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
fun StatTypeSelector(
    selectedType: StatType,
    onTypeSelected: (StatType) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll when selection changes
    LaunchedEffect(selectedType) {
        val selectedIndex = StatType.values().indexOf(selectedType)
        if (selectedIndex != -1) {
            coroutineScope.launch {
                listState.animateScrollToItem(selectedIndex)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.wrapContentWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            LazyRow(
                state = listState,
                modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 0.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(StatType.values()) { type ->
                    val isSelected = type == selectedType

                    Button(
                        onClick = { onTypeSelected(type) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color.Black else Color.Transparent,
                            contentColor = if (isSelected) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(32.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = type.displayName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelector(
    categories: List<WorkoutCategory>,
    selectedCategory: WorkoutCategory?,
    onCategorySelected: (WorkoutCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Button(
            onClick = { showDropdown = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = LightGrayBackground,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory != null) {
                        "Kategoria: ${selectedCategory.name}"
                    } else {
                        "Kategoria: Wszystkie"
                    }
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        }

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false }
        ) {
            DropdownMenuItem(
                text = { Text("Wszystkie") },
                onClick = {
                    onCategorySelected(null)
                    showDropdown = false
                }
            )

            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category)
                        showDropdown = false
                    }
                )
            }
        }
    }
}

@Composable
fun ExerciseSelector(
    selectedExercise: Exercise?,
    onExerciseSelected: (Exercise?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showExerciseSelection by remember { mutableStateOf(false) }

    Button(
        onClick = { showExerciseSelection = true },
        colors = ButtonDefaults.buttonColors(
            containerColor = LightGrayBackground,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedExercise != null) {
                    "Ćwiczenie: ${selectedExercise.name}"
                } else {
                    "Wybierz ćwiczenie"
                }
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
    }

    if (showExerciseSelection) {
        ExerciseSelectionBottomSheet(
            onDismiss = { showExerciseSelection = false },
            onExercisesSelected = { exercises ->
                if (exercises.isNotEmpty()) {
                    // Convert first exercise to Exercise model
                    val exercise = Exercise(
                        id = exercises.first().id, // Fixed: changed from exerciseId to id
                        name = exercises.first().name,
                        category = exercises.first().category,
                        primaryMuscles = emptyList(),
                        secondaryMuscles = emptyList(),
                        instructions = emptyList(),
                        equipment = "",
                        // Removed: difficulty parameter (not present in Exercise model)
                        force = "",
                        mechanic = ""
                    )
                    onExerciseSelected(exercise)
                }
                showExerciseSelection = false
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black
            )
        }
    }
}

@Composable
fun PersonalRecordCard(
    weight: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gold medal icon placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFFFD700), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("★", fontSize = 20.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(R.string.personal_record),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = weight,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                )
            }
        }
    }
}

// Add this to ChartComponents.kt file at the beginning, after imports

@Composable
fun ExerciseChipFilter(
    exerciseId: String,
    exerciseName: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onToggle,
        label = { Text(exerciseName) },
        selected = isSelected,
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color.Black,
            selectedLabelColor = Color.White,
            containerColor = Color(0xFFF5F5F5), // LightGrayBackground equivalent
            labelColor = Color.Black
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold
        ),
        modifier = modifier.padding(vertical = 16.dp)
    )
}