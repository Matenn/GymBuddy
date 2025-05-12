package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
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

    // Local copy of the workout for editing - remember both workout and exercises changes
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
                            },
                            onExerciseDeleted = {
                                // Remove the exercise from the list
                                exercises.removeAt(index)

                                // Update the workout in the ViewModel
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
    onExerciseDeleted: () -> Unit,
    workoutViewModel: WorkoutViewModel = hiltViewModel()
) {
    val sets = remember { mutableStateListOf<ExerciseSet>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) } // For delete confirmation dialog

    // Initialize sets from exercise
    LaunchedEffect(exercise) {
        sets.clear()
        sets.addAll(exercise.sets)
    }

    // Get previous exercise stats
    val exerciseStats by workoutViewModel.getExerciseStatsForId(exercise.exerciseId).collectAsState(null)
    val previousSetsMap by workoutViewModel.previousSetsMap.collectAsState()

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_exercise)) },
            text = { Text(stringResource(R.string.delete_exercise_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onExerciseDeleted()
                    }
                ) {
                    Text(stringResource(R.string.confirm), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Exercise name with delete option
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // Delete exercise button
            IconButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete_exercise),
                    tint = Color.Red
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Column headers - only shown if there are sets
        if (sets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Match the width of the set type button
                Box(modifier = Modifier.width(60.dp)) {
                    Text(
                        text = stringResource(R.string.seria),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                // Previous values column
                Text(
                    text = stringResource(R.string.poprzednio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )

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

                // Empty space for delete button
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Exercise sets
        sets.forEachIndexed { index, set ->
            // Calculate normal set number
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

        // Add set button - styled like in mockup
        Button(
            onClick = {
                // Add a new set with empty initial values
                sets.add(
                    ExerciseSet(
                        setType = "normal",
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
                .padding(vertical = 8.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF5F5F5),
                contentColor = Color.Black
            )
        ) {
            Text(
                text = stringResource(R.string.dodaj_serie),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
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
    // Initialize with empty strings if values are 0
    var weight by remember {
        mutableStateOf(if (set.weight <= 0.0) "" else set.weight.toString())
    }
    var reps by remember {
        mutableStateOf(if (set.reps <= 0) "" else set.reps.toString())
    }
    var showSetTypeMenu by remember { mutableStateOf(false) }

    // Find previous set based on type and number
    val previousSet = previousSets?.find { prevSet ->
        if (setType == "normal") {
            prevSet.setType == setType && prevSet.normalSetNumber == normalSetNumber
        } else {
            prevSet.setType == setType
        }
    }

    // Determine previous values to show as hints
    val previousWeight = previousSet?.weight?.toString() ?: "0.0"
    val previousReps = previousSet?.reps?.toString() ?: "0"

    // Format previous values for display
    val previousDisplay = if (previousSet != null && (previousSet.weight > 0 || previousSet.reps > 0)) {
        "${previousSet.weight}kg x ${previousSet.reps}"
    } else {
        "-"
    }

    // Calculate set display label
    val setLabel = when (setType) {
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
        // Set type selector
        Box(modifier = Modifier.width(60.dp)) {
            Button(
                onClick = { showSetTypeMenu = true },
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(0.dp),
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
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    text = setLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
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

        // Previous set info
        Text(
            text = previousDisplay,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )

        // Weight input - styled as plain text
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // On click focus logic
                }
        ) {
            if (weight.isEmpty()) {
                Text(
                    text = previousWeight,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            BasicTextField(
                value = weight,
                onValueChange = {
                    weight = it
                    val weightValue = it.toDoubleOrNull() ?: 0.0
                    val repsValue = reps.toIntOrNull() ?: 0
                    onSetUpdated(set.copy(
                        setType = setType,
                        weight = weightValue,
                        reps = repsValue
                    ))
                },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Reps input - styled as plain text
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // On click focus logic
                }
        ) {
            if (reps.isEmpty()) {
                Text(
                    text = previousReps,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            BasicTextField(
                value = reps,
                onValueChange = {
                    reps = it
                    val repsValue = it.toIntOrNull() ?: 0
                    val weightValue = weight.toDoubleOrNull() ?: 0.0
                    onSetUpdated(set.copy(
                        setType = setType,
                        weight = weightValue,
                        reps = repsValue
                    ))
                },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Delete button - Now always shown regardless of totalSets
        IconButton(
            onClick = onSetDeleted,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = stringResource(R.string.delete_set),
                tint = Color.Gray
            )
        }
    }
}