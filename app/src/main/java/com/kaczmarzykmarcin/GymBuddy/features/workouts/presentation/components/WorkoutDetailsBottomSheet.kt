package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.data.model.CompletedExercise
import com.kaczmarzykmarcin.GymBuddy.data.model.CompletedWorkout
import com.kaczmarzykmarcin.GymBuddy.data.model.ExerciseSet
import com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components.ExerciseSelectionBottomSheet
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.viewmodel.WorkoutViewModel
import com.kaczmarzykmarcin.GymBuddy.utils.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailsBottomSheet(
    workout: CompletedWorkout,
    onDismiss: () -> Unit,
    onWorkoutUpdate: (CompletedWorkout) -> Unit,
    workoutViewModel: WorkoutViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

    // State tracking
    var isEditMode by remember { mutableStateOf(false) }
    var showExerciseSelection by remember { mutableStateOf(false) }
    var showNameEditDialog by remember { mutableStateOf(false) }
    var showDiscardChangesDialog by remember { mutableStateOf(false) }
    var workoutName by remember { mutableStateOf(workout.name) }

    // Local state for workout exercises - initialize with the current workout exercises
    val exercises = remember(workout.id, isEditMode) { mutableStateListOf<CompletedExercise>().apply {
        addAll(workout.exercises)
    }}

    // Local copy of the workout for editing - remember both workout and exercises changes
    val editedWorkout = remember(workout, exercises, workoutName) {
        workout.copy(
            name = workoutName,
            exercises = exercises.toList()
        )
    }

    // Format the duration for display
    val formattedDuration = remember(workout.duration) {
        formatDuration(workout.duration)
    }

    // Load exercise stats
    LaunchedEffect(workout) {
        // Load exercises data (for previous stats)
        workoutViewModel.loadExerciseStats(workout.userId)

        // Ładuj dane o poprzednich seriach (przydatne w trybie edycji)
        workoutViewModel.loadPreviousSetsData(workout.userId)
    }

    // Handle dialogs
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

    if (showDiscardChangesDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardChangesDialog = false },
            title = { Text(stringResource(R.string.discard_changes)) },
            text = { Text(stringResource(R.string.discard_changes_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardChangesDialog = false
                    isEditMode = false
                    // Reset to original values
                    workoutName = workout.name
                    exercises.clear()
                    exercises.addAll(workout.exercises)
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardChangesDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (isEditMode && (workout.exercises != exercises || workout.name != workoutName)) {
                showDiscardChangesDialog = true
            } else {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = Modifier.padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues())
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top controls
            // Fixed top controls section for WorkoutDetailsBottomSheet
// Replace the existing top controls Row with this:

// Top controls - tylko przyciski
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close/Cancel button with consistent padding
                Box(
                    modifier = Modifier.width(48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isEditMode) {
                        IconButton(onClick = {
                            if (workout.exercises != exercises || workout.name != workoutName) {
                                showDiscardChangesDialog = true
                            } else {
                                isEditMode = false
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel)
                            )
                        }
                    } else {
                        IconButton(onClick = { onDismiss() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    }
                }

                // Pusty środek
                Spacer(modifier = Modifier.weight(1f))

                // Edit/Save button with consistent padding
                Box(
                    modifier = Modifier.width(80.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (isEditMode) {
                        Button(
                            onClick = {
                                // Save changes
                                val updatedWorkout = workout.copy(
                                    name = workoutName,
                                    exercises = exercises.toList()
                                )
                                onWorkoutUpdate(updatedWorkout)
                                isEditMode = false
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black
                            )
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    } else {
                        Button(
                            onClick = { isEditMode = true },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black
                            )
                        ) {
                            Text(stringResource(R.string.edit))
                        }
                    }
                }
            }

// Czas trwania treningu - wyświetlany nad tytułem (tylko w trybie podglądu)
            if (!isEditMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),

                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedDuration,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Workout name with edit button (edycja nazwy dostępna tylko w trybie edycji)
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
                if (isEditMode) {
                    IconButton(onClick = { showNameEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_workout_name)
                        )
                    }
                }
            }

            // Main content (exercise list)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (exercises.isEmpty()) {
                    // Empty state
                    EmptyWorkoutState()
                } else {
                    // List of exercises
                    exercises.forEachIndexed { index, exercise ->
                        if (isEditMode) {
                            // W trybie edycji używamy istniejącego komponentu
                            ExerciseItemWorkout(
                                exercise = exercise,
                                userId = workout.userId,
                                onExerciseUpdated = { updatedExercise ->
                                    exercises[index] = updatedExercise
                                },
                                onExerciseDeleted = {
                                    exercises.removeAt(index)
                                }
                            )
                        } else {
                            // W trybie podglądu używamy ReadOnlyExerciseItem
                            ReadOnlyExerciseItem(exercise = exercise)
                        }

                        if (index < exercises.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }
                }

                // Extra space at bottom
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom buttons (tylko w trybie edycji)
            if (isEditMode) {
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
                showExerciseSelection = false
            }
        )
    }
}

@Composable
fun ReadOnlyExerciseItem(
    exercise: CompletedExercise
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Exercise name
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Column headers
        if (exercise.sets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Set type column
                Box(modifier = Modifier.width(60.dp)) {
                    Text(
                        text = stringResource(R.string.seria),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                // Weight column
                Text(
                    text = stringResource(R.string.ciezar),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                // Reps column
                Text(
                    text = stringResource(R.string.powtorzenia),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Exercise sets
        exercise.sets.forEachIndexed { index, set ->
            // Calculate normal set number for display
            val normalSetNumber = exercise.sets.take(index + 1).count { it.setType == "normal" }

            ReadOnlySetItem(
                set = set,
                normalSetNumber = if (set.setType == "normal") normalSetNumber else 0
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ReadOnlySetItem(
    set: ExerciseSet,
    normalSetNumber: Int
) {
    // Calculate set display label
    val setLabel = when (set.setType) {
        "warmup" -> "W"
        "dropset" -> "D"
        "failure" -> "F"
        else -> normalSetNumber.toString()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set type indicator
        Box(modifier = Modifier.width(60.dp)) {
            Button(
                onClick = { /* Read only - no action */ },
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (set.setType) {
                        "warmup" -> Color(0xFFE3F2FD)  // Jasnoniebieski
                        "dropset" -> Color(0xFFFBE9E7)  // Jasnopomarańczowy
                        "failure" -> Color(0xFFFFF3E0)  // Jasnożółty
                        else -> Color(0xFFEFEFEF)      // Jasnoszary
                    },
                    contentColor = when (set.setType) {
                        "warmup" -> Color.Blue         // Niebieski
                        "dropset" -> Color.Red         // Czerwony
                        "failure" -> Color(0xFFF57F17) // Pomarańczowy
                        else -> Color.Black            // Czarny
                    },
                    disabledContainerColor = when (set.setType) {
                        "warmup" -> Color(0xFFE3F2FD)  // Jasnoniebieski
                        "dropset" -> Color(0xFFFBE9E7)  // Jasnopomarańczowy
                        "failure" -> Color(0xFFFFF3E0)  // Jasnożółty
                        else -> Color(0xFFEFEFEF)      // Jasnoszary
                    },
                    disabledContentColor = when (set.setType) {
                        "warmup" -> Color.Blue         // Niebieski
                        "dropset" -> Color.Red         // Czerwony
                        "failure" -> Color(0xFFF57F17) // Pomarańczowy
                        else -> Color.Black            // Czarny
                    }
                ),
                modifier = Modifier.size(48.dp),
                enabled = false
            ) {
                Text(
                    text = setLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        // Weight value
        Text(
            text = "${set.weight} kg",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        // Reps value
        Text(
            text = "${set.reps}",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
    }
}

// Helper function to format duration
private fun formatDuration(seconds: Long): String {
    return TimeUtils.formatDurationSeconds(seconds)
}