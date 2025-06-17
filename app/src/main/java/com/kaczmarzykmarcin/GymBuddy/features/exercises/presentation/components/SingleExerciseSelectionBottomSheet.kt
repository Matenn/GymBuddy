package com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.features.exercises.domain.model.Exercise
import com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.ExerciseLibraryViewModel
import com.kaczmarzykmarcin.GymBuddy.core.presentation.theme.DarkGray
import com.kaczmarzykmarcin.GymBuddy.core.presentation.theme.LightGrayBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleExerciseSelectionBottomSheet(
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

    // States from ViewModel
    val exercisesGroupedByLetter by viewModel.exercisesGrouped.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val availableMuscleGroups by viewModel.availableMuscleGroups.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val selectedMuscleGroups by viewModel.selectedMuscleGroups.collectAsState()

    val isSearchActive = searchQuery.isNotEmpty()
    val isFilteringActive = selectedCategories.isNotEmpty() || selectedMuscleGroups.isNotEmpty()

    // Local states for filters
    var showCategoryFilter by remember { mutableStateOf(false) }
    var showMuscleGroupFilter by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val lazyListState = rememberLazyListState()

    // Initialize data
    LaunchedEffect(Unit) {
        viewModel.loadExercises()
        viewModel.loadCategories()
        viewModel.loadMuscleGroups()
    }

    // Group filtered results alphabetically
    val filteredExercisesGroupedByLetter = remember(searchResults) {
        searchResults.groupBy { exercise ->
            exercise.name.first().uppercase()
        }.toSortedMap()
    }

    // Use appropriate data based on filtering state
    val displayedExercisesGrouped = if (isSearchActive || isFilteringActive) {
        filteredExercisesGroupedByLetter
    } else {
        exercisesGroupedByLetter
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.select_exercise),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

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
                        contentDescription = "Close"
                    )
                }
            }

            Divider()

            // Search field
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

            // Filter buttons
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
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (selectedCategories.isEmpty()) stringResource(R.string.category_filter_title)
                        else stringResource(R.string.category_filter_title) + " (${selectedCategories.size})",
                        fontWeight = if (selectedCategories.isEmpty()) FontWeight.Normal else FontWeight.Bold
                    )
                }

                Button(
                    onClick = { showMuscleGroupFilter = true },
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightGrayBackground,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (selectedMuscleGroups.isEmpty()) stringResource(R.string.body_part_filter_title)
                        else stringResource(R.string.body_part_filter_title) + " (${selectedMuscleGroups.size})",
                        fontWeight = if (selectedMuscleGroups.isEmpty()) FontWeight.Normal else FontWeight.Bold
                    )
                }
            }

            // Exercise list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if ((isSearchActive || isFilteringActive) && searchResults.isEmpty()) {
                    // Empty state
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
                    // Exercise list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = lazyListState
                    ) {
                        items(displayedExercisesGrouped.entries.sortedBy { it.key }.toList()) { (letter, exercises) ->
                            // Section header
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
                                            isSelected = false, // No selection state needed
                                            selectionMode = false, // Single selection, no checkboxes
                                            onClick = {
                                                // Convert to Exercise model and return
                                                val selectedExercise = Exercise(
                                                    id = exercise.id,
                                                    name = exercise.name,
                                                    category = exercise.category,
                                                    primaryMuscles = exercise.primaryMuscles,
                                                    secondaryMuscles = exercise.secondaryMuscles,
                                                    instructions = exercise.instructions,
                                                    equipment = exercise.equipment,
                                                    force = exercise.force,
                                                    mechanic = exercise.mechanic
                                                )
                                                onExerciseSelected(selectedExercise)
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

                        // Bottom spacing
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }

                // Alphabetical scrollbar (reuse from existing component)
                val lettersList = remember(displayedExercisesGrouped) {
                    displayedExercisesGrouped.keys.sorted()
                }

                val letterToPositionMap = remember(displayedExercisesGrouped) {
                    val sections = displayedExercisesGrouped.entries.sortedBy { it.key }
                    val map = mutableMapOf<String, Int>()
                    sections.forEachIndexed { index, (letter, _) ->
                        map[letter] = index
                    }
                    map
                }

                if (lettersList.isNotEmpty()) {
                    AlphabeticalScrollBar(
                        letters = lettersList,
                        lazyListState = lazyListState,
                        letterIndexMap = letterToPositionMap,
                        compact = true, // Use compact version for bottom sheet
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                    )
                }
            }
        }

        // Filter bottom sheets
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
    }
}