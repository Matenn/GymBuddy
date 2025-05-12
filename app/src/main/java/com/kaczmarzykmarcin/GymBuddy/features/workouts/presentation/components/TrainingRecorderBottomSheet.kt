package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.core.data.model.PreviousSetInfo
import com.kaczmarzykmarcin.GymBuddy.data.model.CompletedExercise
import com.kaczmarzykmarcin.GymBuddy.data.model.CompletedWorkout
import com.kaczmarzykmarcin.GymBuddy.data.model.Exercise
import com.kaczmarzykmarcin.GymBuddy.data.model.ExerciseSet
import com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components.ExerciseSelectionBottomSheet
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.viewmodel.WorkoutViewModel
import com.kaczmarzykmarcin.GymBuddy.utils.TimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingRecorderBottomSheet(
    workout: CompletedWorkout,
    onDismiss: () -> Unit,
    onWorkoutFinish: (CompletedWorkout) -> Unit,
    onWorkoutCancel: (CompletedWorkout) -> Unit,
    workoutViewModel: WorkoutViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

    var showExerciseSelection by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember { mutableStateOf(false) }
    var showNameEditDialog by remember { mutableStateOf(false) }
    var workoutName by remember { mutableStateOf(workout.name) }

    // Local state for workout exercises - initialize with the current workout exercises
    val exercises = remember(workout.id) { mutableStateListOf<CompletedExercise>().apply {
        addAll(workout.exercises)
    }}

    // Added timeState to force recomposition
    var timeState by remember { mutableStateOf(0L) }

    // Format the elapsed time
    val elapsedTime = remember(workout, timeState) {
        derivedStateOf {
            val currentTime = System.currentTimeMillis()
            val startTime = workout.startTime.toDate().time
            val elapsedMillis = currentTime - startTime
            TimeUtils.formatElapsedTime(elapsedMillis)
        }
    }

    // Local copy of the workout for editing - remeber both workout and exercises changes
    val currentWorkout = remember(workout, exercises, workoutName) {
        workout.copy(
            name = workoutName,
            exercises = exercises.toList()
        )
    }

    // When workout changes (e.g., after adding exercises and persisting), update exercises list
    LaunchedEffect(workout) {
        // Update exercises from workout if they are different
        // This is important to handle cases where exercises are updated externally
        val workoutExercisesIds = workout.exercises.map { it.exerciseId }.toSet()
        val localExercisesIds = exercises.map { it.exerciseId }.toSet()

        // If there are differences, completely update the exercises list
        if (workoutExercisesIds != localExercisesIds || workout.exercises.size != exercises.size) {
            exercises.clear()
            exercises.addAll(workout.exercises)
        }

        // Load exercises data (for previous stats)
        workoutViewModel.loadExerciseStats(workout.userId)
    }

    // Update workout state after changes to exercises
    LaunchedEffect(exercises.size) {
        // When exercises change, update the workout in the ViewModel to persist changes
        if (exercises.isNotEmpty() && workout.exercises.size != exercises.size) {
            val updatedWorkout = workout.copy(exercises = exercises.toList())
            workoutViewModel.updateWorkout(updatedWorkout)
        }
    }

    // Update workout time every second for time tracking
    LaunchedEffect(Unit) {
        while(true) {
            delay(1000)
            timeState = System.currentTimeMillis() // This forces recomposition
        }
    }

    // Załaduj dane o poprzednich seriach
    LaunchedEffect(workout.userId) {
        workoutViewModel.loadPreviousSetsData(workout.userId)
    }


    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text(stringResource(R.string.cancel_workout)) },
            text = { Text(stringResource(R.string.cancel_workout_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirmation = false
                        onWorkoutCancel(currentWorkout)
                    }
                ) {
                    Text(stringResource(R.string.confirm), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showNameEditDialog) {
        AlertDialog(
            onDismissRequest = { showNameEditDialog = false },
            title = { Text(stringResource(R.string.edit_workout_name)) },
            text = {
                OutlinedTextField(
                    value = workoutName,
                    onValueChange = { workoutName = it },
                    label = { Text(stringResource(R.string.workout_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNameEditDialog = false
                    // Update workout in ViewModel when name changes
                    if (workoutName != workout.name) {
                        workoutViewModel.updateWorkout(
                            workout.copy(name = workoutName)
                        )
                    }
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    workoutName = workout.name
                    showNameEditDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }


    ModalBottomSheet(
        onDismissRequest = {
            // Update workout in ViewModel before dismissing
            if (exercises.isNotEmpty() && (workout.exercises != exercises || workout.name != workoutName)) {
                val updatedWorkout = workout.copy(
                    name = workoutName,
                    exercises = exercises.toList()
                )
                workoutViewModel.updateWorkout(updatedWorkout)
            }
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = Modifier.padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues())
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Elapsed time
                Text(
                    text = elapsedTime.value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                // Finish button (enabled only if there's at least one exercise)
                Button(
                    onClick = {
                        val updatedWorkout = workout.copy(
                            name = workoutName,
                            exercises = exercises.toList(),
                            endTime = com.google.firebase.Timestamp.now(),
                            duration = ((System.currentTimeMillis() - workout.startTime.toDate().time) / 60000) // Convert to minutes
                        )
                        onWorkoutFinish(updatedWorkout)
                    },
                    enabled = exercises.isNotEmpty(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        disabledContainerColor = Color.Gray
                    )
                ) {
                    Text(stringResource(R.string.finish))
                }
            }

            // Workout name with edit button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workoutName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showNameEditDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_workout_name)
                    )
                }
            }

            // Main content (exercise list) - Now uses weight to take remaining space
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes remaining space
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (exercises.isEmpty()) {
                    // Empty state
                    EmptyWorkoutState()
                } else {
                    // List of exercises
                    exercises.forEachIndexed { index, exercise ->
                        ExerciseItem(
                            exercise = exercise,
                            userId = workout.userId,
                            onExerciseUpdated = { updatedExercise ->
                                exercises[index] = updatedExercise
                                // Update workout in ViewModel when exercise changes
                                val updatedWorkout = workout.copy(
                                    exercises = exercises.toList()
                                )
                                workoutViewModel.updateWorkout(updatedWorkout)
                            }
                        )

                        if (index < exercises.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }
                }
            }

            // Bottom buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Add exercise button
                Button(
                    onClick = { showExerciseSelection = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black
                    )
                ) {
                    Text(text = stringResource(R.string.add_exercise))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cancel workout button
                Button(
                    onClick = { showCancelConfirmation = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEFF1F5),
                        contentColor = Color.Red
                    )
                ) {
                    Text(text = stringResource(R.string.cancel_workout))
                }
            }
        }
    }

    // Exercise selection sheet
    if (showExerciseSelection) {
        ExerciseSelectionBottomSheet(
            onDismiss = { showExerciseSelection = false },
            onExercisesSelected = { selectedExercises ->
                // Konwertuj do CompletedExercise i dodawaj do listy
                val newExercises = selectedExercises.map { exercise ->
                    CompletedExercise(
                        exerciseId = exercise.id,
                        name = exercise.name,
                        category = exercise.category,
                        sets = listOf(
                            ExerciseSet(
                                setType = "warmup",
                                weight = 0.0,
                                reps = 0
                            )
                        )
                    )
                }
                exercises.addAll(newExercises)

                val updatedWorkout = workout.copy(
                    exercises = exercises.toList()
                )
                workoutViewModel.updateWorkout(updatedWorkout)

                showExerciseSelection = false
            }
        )
    }
}


@Composable
fun EmptyWorkoutState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_logo_barbell),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.add_exercise_to_start),
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}

@Composable
fun ExerciseItem(
    exercise: CompletedExercise,
    userId: String,
    onExerciseUpdated: (CompletedExercise) -> Unit,
    workoutViewModel: WorkoutViewModel = hiltViewModel()
) {
    val sets = remember { mutableStateListOf<ExerciseSet>() }

    // Initialize sets from exercise
    LaunchedEffect(exercise) {
        sets.clear()
        sets.addAll(exercise.sets)
    }

    // Get previous exercise stats
    val exerciseStats by workoutViewModel.getExerciseStatsForId(exercise.exerciseId).collectAsState(null)
    val previousSetsMap by workoutViewModel.previousSetsMap.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Exercise name
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Exercise sets
        sets.forEachIndexed { index, set ->
            // Oblicz numer serii normalnej, zliczając tylko serie typu "normal" do aktualnego indeksu
            val normalSetNumber = sets.take(index + 1).count { it.setType == "normal" }

            SetItem(
                set = set,
                index = index,
                normalSetNumber = if (set.setType == "normal") normalSetNumber else 0,
                totalSets = sets.size,
                previousSets = previousSetsMap[exercise.exerciseId],
                previousStats = exerciseStats,
                onSetUpdated = { updatedSet ->
                    sets[index] = updatedSet
                    onExerciseUpdated(
                        exercise.copy(sets = sets.toList())
                    )
                },
                onSetDeleted = {
                    sets.removeAt(index)
                    onExerciseUpdated(
                        exercise.copy(sets = sets.toList())
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add set button
        Button(
            onClick = {
                // Add a new set with appropriate set number
                val newSetType = "normal"

                sets.add(
                    ExerciseSet(
                        setType = newSetType,
                        weight = 0.0,
                        reps = 0
                    )
                )

                onExerciseUpdated(
                    exercise.copy(sets = sets.toList())
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEFF1F5),
                contentColor = Color.Black
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.add_set))
            }
        }
    }
}

@Composable
fun SetItem(
    set: ExerciseSet,
    index: Int,
    normalSetNumber: Int,
    totalSets: Int,
    previousSets: List<PreviousSetInfo>?,
    previousStats: Map<String, Any?>?,
    onSetUpdated: (ExerciseSet) -> Unit,
    onSetDeleted: () -> Unit
) {
    var setType by remember { mutableStateOf(set.setType) }
    var weight by remember { mutableStateOf(set.weight.toString()) }
    var reps by remember { mutableStateOf(set.reps.toString()) }
    var showSetTypeMenu by remember { mutableStateOf(false) }

    // Znajdź poprzednią serię na podstawie typu i numeru serii
    val previousSet = previousSets?.find { prevSet ->
        if (setType == "normal") {
            // Dla serii normalnych, znajdź po numerze serii
            prevSet.setType == setType && prevSet.normalSetNumber == normalSetNumber
        } else {
            // Dla innych typów serii (warmup, dropset, failure), znajdź po typie
            prevSet.setType == setType
        }
    }

    // Determine previous values to show as hints
    val previousWeight = previousSet?.weight?.toString() ?: "0.0"
    val previousReps = previousSet?.reps?.toString() ?: "0"

    // Calculate set display label
    val setLabel = when (setType) {
        "warmup" -> "W"
        "dropset" -> "D"
        "failure" -> "F"
        else -> normalSetNumber.toString() // Używamy numeru serii normalnej zamiast indeksu
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F8F8)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set type selector
            Box {
                Button(
                    onClick = { showSetTypeMenu = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (setType) {
                            "warmup" -> Color(0xFFE3F2FD)
                            "dropset" -> Color(0xFFFBE9E7)
                            "failure" -> Color(0xFFFFF3E0)
                            else -> Color(0xFFEFEFEF)
                        },
                        contentColor = when (setType) {
                            "warmup" -> Color.Blue
                            "dropset" -> Color.Red
                            "failure" -> Color(0xFFF57F17)
                            else -> Color.Black
                        }
                    ),
                    modifier = Modifier.size(width = 48.dp, height = 48.dp)
                ) {
                    Text(
                        text = setLabel,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = showSetTypeMenu,
                    onDismissRequest = { showSetTypeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.normal_set)) },
                        onClick = {
                            setType = "normal"
                            onSetUpdated(set.copy(setType = "normal"))
                            showSetTypeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.warmup_set)) },
                        onClick = {
                            setType = "warmup"
                            onSetUpdated(set.copy(setType = "warmup"))
                            showSetTypeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dropset)) },
                        onClick = {
                            setType = "dropset"
                            onSetUpdated(set.copy(setType = "dropset"))
                            showSetTypeMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.failure_set)) },
                        onClick = {
                            setType = "failure"
                            onSetUpdated(set.copy(setType = "failure"))
                            showSetTypeMenu = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Previous set info (if available)
            Text(
                text = if (previousSet != null && (previousSet.weight > 0 || previousSet.reps > 0)) {
                    "${previousSet.weight} x ${previousSet.reps}"
                } else {
                    "-"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Weight input
            OutlinedTextField(
                value = weight,
                onValueChange = {
                    weight = it
                    val weightValue = it.toDoubleOrNull() ?: 0.0
                    val repsValue = reps.toIntOrNull() ?: 0
                    // Użyj aktualnych wartości ze stanu lokalnego
                    onSetUpdated(set.copy(
                        setType = setType,
                        weight = weightValue,
                        reps = repsValue
                    ))
                },
                placeholder = { Text(previousWeight) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Reps input
            OutlinedTextField(
                value = reps,
                onValueChange = {
                    reps = it
                    val repsValue = it.toIntOrNull() ?: 0
                    val weightValue = weight.toDoubleOrNull() ?: 0.0

                    Log.d("SetEditor", "Input reps: '$it', Converted to int: $repsValue")

                    // Użyj aktualnych wartości ze stanu lokalnego
                    onSetUpdated(set.copy(
                        setType = setType,
                        weight = weightValue,
                        reps = repsValue
                    ))
                },
                placeholder = { Text(previousReps) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )

            // Delete button
            if (totalSets > 1) {
                IconButton(onClick = onSetDeleted) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.delete_set),
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}



@Composable
fun SelectableExerciseItem(
    exercise: Exercise,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onToggle() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Exercise info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = exercise.primaryMuscles.firstOrNull()?.capitalizeFirstLetter() ?: exercise.category,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // Selection indicator
        if (isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = stringResource(R.string.selected),
                tint = Color.Green
            )
        }
    }
}

@Composable
fun FilterButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5),
            contentColor = if (isSelected) Color.Blue else Color.Black
        ),
        modifier = modifier.height(42.dp)
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ActiveWorkoutMiniBar has been moved to a separate file

// Extension function to capitalize first letter of a string
private fun String.capitalizeFirstLetter(): String {
    return if (this.isEmpty()) this else this.substring(0, 1).uppercase() + this.substring(1)
}

// Helper functions for time formatting have been moved to a separate file