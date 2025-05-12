package com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.data.model.Exercise
import com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.ExerciseLibraryViewModel
import com.kaczmarzykmarcin.GymBuddy.ui.theme.DarkGray
import com.kaczmarzykmarcin.GymBuddy.ui.theme.LightGrayBackground

@Composable
fun ExerciseLibraryContent(
    viewModel: ExerciseLibraryViewModel = hiltViewModel(),
    selectionMode: Boolean = false,
    showHeader: Boolean = true,
    onBackPressed: (() -> Unit)? = null,
    onExercisesSelected: ((List<Exercise>) -> Unit)? = null
) {
    // Istniejące stany
    val exercisesGroupedByLetter by viewModel.exercisesGrouped.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val availableMuscleGroups by viewModel.availableMuscleGroups.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val selectedMuscleGroups by viewModel.selectedMuscleGroups.collectAsState()
    val isSearchActive = searchQuery.isNotEmpty()
    val isFilteringActive = selectedCategories.isNotEmpty() || selectedMuscleGroups.isNotEmpty()

    // Nowe stany dla trybu wyboru
    val selectedExercises by viewModel.selectedExercises.collectAsState()

    // Pokazywanie kategorii i grup mięśniowych w bottom sheet
    var showCategoryFilter by remember { mutableStateOf(false) }
    var showMuscleGroupFilter by remember { mutableStateOf(false) }

    // Ustawienie trybu wyboru
    LaunchedEffect(selectionMode) {
        viewModel.setSelectionMode(selectionMode)
    }

    // Inicjalizacja danych
    LaunchedEffect(Unit) {
        viewModel.loadExercises()
        viewModel.loadCategories()
        viewModel.loadMuscleGroups()
    }

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

    val focusManager = LocalFocusManager.current

    // Używamy LazyListState dla obsługi AlphabeticalScrollBar
    val lazyListState = rememberLazyListState()

    // Lista liter do paska przewijania
    val lettersList = remember(displayedExercisesGrouped) {
        displayedExercisesGrouped.keys.sorted()
    }

    // Mapowanie litery do pozycji w liście
    val letterToPositionMap = remember(displayedExercisesGrouped) {
        val sections = displayedExercisesGrouped.entries.sortedBy { it.key }
        val map = mutableMapOf<String, Int>()

        sections.forEachIndexed { index, (letter, _) ->
            map[letter] = index
        }

        map
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Nagłówek jeśli wymagany
            if (showHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!selectionMode) {
                        // Standardowy nagłówek dla trybu przeglądania
                        Text(
                            text = stringResource(R.string.exercises_title),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                            )
                        )
                    } else {
                        // Nagłówek dla trybu wyboru
                        Text(
                            text = stringResource(R.string.select_exercises),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Przyciski dla trybu wyboru
                        if (onBackPressed != null) {
                            TextButton(
                                onClick = { onBackPressed() }
                            ) {
                                Text(
                                    text = stringResource(R.string.cancel),
                                    color = Color.Red
                                )
                            }
                        }

                        if (onExercisesSelected != null) {
                            TextButton(
                                onClick = {
                                    onExercisesSelected(selectedExercises.toList())
                                },
                                enabled = selectedExercises.isNotEmpty()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.add) + (if (selectedExercises.isNotEmpty()) " (${selectedExercises.size})" else ""),
                                        color = if (selectedExercises.isEmpty()) Color.Gray else Color.Blue
                                    )
                                }
                            }
                        }
                    }
                }

                Divider()
            }

            // Pole wyszukiwania
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text(stringResource(R.string.search_exercise)) },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(50.dp)
            )

            // Przyciski filtrów (kategorie i grupy mięśniowe)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                        text = if (selectedCategories.isEmpty()) stringResource(R.string.category_filter_title)
                        else stringResource(R.string.category_filter_title) + " (${selectedCategories.size})",
                        fontWeight = if (selectedCategories.isEmpty()) FontWeight.Normal
                        else FontWeight.Bold
                    )
                }

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
                        text = if (selectedMuscleGroups.isEmpty()) stringResource(R.string.body_part_filter_title)
                        else stringResource(R.string.body_part_filter_title) + " (${selectedMuscleGroups.size})",
                        fontWeight = if (selectedMuscleGroups.isEmpty()) FontWeight.Normal
                        else FontWeight.Bold
                    )
                }
            }

            // Lista ćwiczeń - umieszczamy w Box, aby móc dodać scrollbar obok listy
            Box(
                modifier = Modifier.weight(1f)
            ) {
                // Lista ćwiczeń
                if ((isSearchActive || isFilteringActive) && searchResults.isEmpty()) {
                    // Pokazuj stan pusty gdy brak wyników
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
                    // Lista ćwiczeń z podziałem na sekcje alfabetyczne
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = lazyListState
                    ) {
                        items(displayedExercisesGrouped.entries.sortedBy { it.key }.toList()) { (letter, exercises) ->
                            // Nagłówek sekcji
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(vertical = 8.dp)
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
                                            isSelected = selectedExercises.contains(exercise),
                                            selectionMode = selectionMode,
                                            onClick = {
                                                if (selectionMode) {
                                                    viewModel.toggleExerciseSelection(exercise)
                                                } else {
                                                    // Nawigacja do szczegółów ćwiczenia w trybie normalnym
                                                }
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

                        // Dodaj miejsce na dole dla bezpiecznego przewijania
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }

                // Dodaj AlphabeticalScrollBar tylko do obszaru listy
                if (lettersList.isNotEmpty()) {
                    AlphabeticalScrollBar(
                        letters = lettersList,
                        lazyListState = lazyListState,
                        letterIndexMap = letterToPositionMap,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                    )
                }
            }
        }

        // Bottom Sheety do filtrowania
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
    }}
