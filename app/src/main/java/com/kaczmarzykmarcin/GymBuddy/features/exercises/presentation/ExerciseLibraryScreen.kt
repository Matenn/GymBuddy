package com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.data.model.Exercise
import com.kaczmarzykmarcin.GymBuddy.features.dashboard.presentation.BottomNavigationBar
import com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components.AlphabeticalScrollBar
import com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components.CategoryFilterBottomSheet
import com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components.MuscleGroupFilterBottomSheet
import com.kaczmarzykmarcin.GymBuddy.ui.theme.DarkGray
import com.kaczmarzykmarcin.GymBuddy.ui.theme.LightGrayBackground

@Composable
fun ExerciseLibraryScreen(
    navController: NavController,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val exercisesGroupedByLetter by viewModel.exercisesGrouped.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val availableMuscleGroups by viewModel.availableMuscleGroups.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val selectedMuscleGroups by viewModel.selectedMuscleGroups.collectAsState()
    val isSearchActive = searchQuery.isNotEmpty()
    val isFilteringActive = selectedCategories.isNotEmpty() || selectedMuscleGroups.isNotEmpty()

    var showCategoryFilter by remember { mutableStateOf(false) }
    var showMuscleGroupFilter by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Grupowanie wyników wyszukiwania/filtrowania alfabetycznie
    val filteredExercisesGroupedByLetter = remember(searchResults) {
        searchResults.groupBy { exercise ->
            exercise.name.first().uppercase()
        }.toSortedMap()
    }

    // Używamy odpowiednich danych w zależności od tego, czy filtrowanie jest aktywne
    val displayedExercisesGrouped = if (isSearchActive || isFilteringActive) {
        filteredExercisesGroupedByLetter
    } else {
        exercisesGroupedByLetter
    }

    // Used for alphabetical scrollbar - zawsze używamy aktualnie wyświetlanych danych
    val letterHeaderIndices = remember(displayedExercisesGrouped) {
        derivedStateOf {
            val indices = mutableMapOf<String, Int>()

            // Każda litera ma dokładnie jeden element (item) w LazyColumn
            displayedExercisesGrouped.keys.sorted().forEachIndexed { index, letter ->
                indices[letter] = index
            }

            indices
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadExercises()
        viewModel.loadCategories()
        viewModel.loadMuscleGroups()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues())
        ) {
            Text(
                text = "Ćwiczenia",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text(stringResource(id=R.string.search_field_exercise_text)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(32.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LightGrayBackground,
                    unfocusedContainerColor = LightGrayBackground,
                    cursorColor = Color.Black,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp).height(50.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Filter buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category filter button
                Button(
                    onClick = { showCategoryFilter = true },
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightGrayBackground,
                        contentColor = if (selectedCategories.isEmpty()) Color.Black else Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (selectedCategories.isEmpty()) stringResource(id=R.string.category_filter_title)
                        else stringResource(id=R.string.category_filter_title) + " (${selectedCategories.size})",
                        fontWeight = if (selectedCategories.isEmpty()) FontWeight.Normal
                        else FontWeight.Bold,
                        //style = TextStyle(fontSize = 14.sp)
                    )
                }

                // Muscle group filter button
                Button(
                    onClick = { showMuscleGroupFilter = true },
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightGrayBackground,
                        contentColor = if (selectedMuscleGroups.isEmpty()) Color.Black else Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (selectedMuscleGroups.isEmpty()) stringResource(id=R.string.body_part_filter_title)
                        else stringResource(id=R.string.body_part_filter_title) + " (${selectedMuscleGroups.size})",
                        fontWeight = if (selectedMuscleGroups.isEmpty()) FontWeight.Normal
                        else FontWeight.Bold,
                        //style = TextStyle(fontSize = 14.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Exercise list
            Box(modifier = Modifier.weight(1f)) {
                if ((isSearchActive || isFilteringActive) && searchResults.isEmpty()) {
                    // Show empty state when no results
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_exercise_found),
                            color = DarkGray
                        )
                    }
                } else {
                    // Zawsze używamy tego samego widoku z podziałem na sekcje alfabetyczne
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Wyświetlamy pogrupowane ćwiczenia (albo wszystkie, albo wyfiltrowane)
                        displayedExercisesGrouped.entries.sortedBy { it.key }.forEach { (letter, exercises) ->
                            item {
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                )

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 2.dp
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        exercises.forEachIndexed { index, exercise ->
                                            ExerciseItem(
                                                exercise = exercise,
                                                onClick = {
                                                    // Will be implemented to navigate to exercise details
                                                }
                                            )

                                            if (index < exercises.size - 1) {
                                                Divider(
                                                    color = Color.LightGray,
                                                    thickness = 0.5.dp,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Add space at the bottom for navigation bar
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }

                    // Alphabetical scroll bar
                    if (displayedExercisesGrouped.isNotEmpty()) {
                        AlphabeticalScrollBar(
                            letters = displayedExercisesGrouped.keys.sorted(),
                            lazyListState = lazyListState,
                            letterIndexMap = letterHeaderIndices.value,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }

        // Bottom Navigation Bar
        BottomNavigationBar(
            navController = navController,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Category Filter Bottom Sheet
        if (showCategoryFilter) {
            CategoryFilterBottomSheet(
                availableCategories = availableCategories,
                selectedCategories = selectedCategories,
                onCategorySelected = { category, isSelected ->
                    viewModel.onCategorySelected(category, isSelected)
                },
                onClearFilters = {
                    viewModel.clearCategoryFilters()
                },
                onDismiss = {
                    showCategoryFilter = false
                }
            )
        }

        // Muscle Group Filter Bottom Sheet
        if (showMuscleGroupFilter) {
            MuscleGroupFilterBottomSheet(
                availableMuscleGroups = availableMuscleGroups,
                selectedMuscleGroups = selectedMuscleGroups,
                onMuscleGroupSelected = { muscleGroup, isSelected ->
                    viewModel.onMuscleGroupSelected(muscleGroup, isSelected)
                },
                onClearFilters = {
                    viewModel.clearMuscleGroupFilters()
                },
                onDismiss = {
                    showMuscleGroupFilter = false
                }
            )
        }
    }
}

@Composable
fun ExerciseItem(
    exercise: Exercise,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Show primary muscle or category based on exercise type
            Text(
                text = if (exercise.category.lowercase() == "cardio")
                    "Cardio"
                else exercise.primaryMuscles.firstOrNull()?.capitalizeFirstLetter() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkGray
            )
        }
    }
}

// Extension function to capitalize first letter
private fun String.capitalizeFirstLetter(): String {
    return if (this.isEmpty()) this else this.substring(0, 1).uppercase() + this.substring(1)
}