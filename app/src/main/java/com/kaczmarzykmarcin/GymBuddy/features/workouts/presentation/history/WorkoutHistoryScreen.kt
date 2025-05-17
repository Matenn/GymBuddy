package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.history

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.core.presentation.components.AppScaffold
import com.kaczmarzykmarcin.GymBuddy.core.presentation.components.rememberContentPadding
import com.kaczmarzykmarcin.GymBuddy.data.model.CompletedWorkout
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.AuthState
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.AuthViewModel
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.components.WorkoutDetailsBottomSheet
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.history.viewmodel.WorkoutHistoryViewModel
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.viewmodel.WorkoutViewModel
import com.kaczmarzykmarcin.GymBuddy.ui.theme.LightGrayBackground
import com.kaczmarzykmarcin.GymBuddy.utils.TimeUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val TAG = "WorkoutHistoryScreen"

/**
 * Punkt wejścia do wstrzykiwania repozytorium
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface WorkoutRepositoryEntryPoint {
    val workoutRepository: com.kaczmarzykmarcin.GymBuddy.data.repository.WorkoutRepository
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    workoutViewModel: WorkoutViewModel = hiltViewModel(),
    workoutHistoryViewModel: WorkoutHistoryViewModel = hiltViewModel()
) {
    // Pobierz repozytorium przez EntryPoint zamiast przez hiltViewModel
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context,
        WorkoutRepositoryEntryPoint::class.java
    )
    val workoutRepository = entryPoint.workoutRepository

    val workoutHistory by workoutHistoryViewModel.workoutHistory.collectAsState()
    val filteredWorkouts by workoutHistoryViewModel.filteredWorkouts.collectAsState()

    val searchQuery by workoutHistoryViewModel.searchQuery.collectAsState()
    val activeDate by workoutHistoryViewModel.activeDate.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val contentPadding = rememberContentPadding(workoutViewModel = workoutViewModel)
    val coroutineScope = rememberCoroutineScope()

    // Zmienne stanu dla szczegółów treningu
    var selectedWorkoutId by remember { mutableStateOf<String?>(null) }
    var selectedWorkout by remember { mutableStateOf<CompletedWorkout?>(null) }
    var isLoadingWorkout by remember { mutableStateOf(false) }

    val currentUser = when (val authState = authViewModel.authState.collectAsState().value) {
        is AuthState.Authenticated -> authState.user
        else -> null
    }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            // Load workout history when user is authenticated
            workoutHistoryViewModel.loadWorkoutHistory(user.uid)
        }
    }

    // Ładuj szczegóły treningu gdy zostanie wybrany
    LaunchedEffect(selectedWorkoutId) {
        selectedWorkoutId?.let { workoutId ->
            isLoadingWorkout = true
            try {
                val result = workoutRepository.getCompletedWorkout(workoutId)
                if (result.isSuccess) {
                    selectedWorkout = result.getOrNull()
                } else {
                    Log.e(TAG, "Error loading workout details: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading workout details", e)
            } finally {
                isLoadingWorkout = false
            }
        }
    }

    AppScaffold(
        navController = navController,
        workoutViewModel = workoutViewModel,
        contentPadding = PaddingValues(bottom = 80.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues())
                .padding(16.dp, 0.dp, 16.dp, 16.dp)
                .padding(paddingValues)
        ) {
            Text(
                text = stringResource(R.string.workout_history),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 16.dp, top = 16.dp)
            )

            // Search bar and calendar button
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { workoutHistoryViewModel.updateSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.search_workouts)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .size(50.dp)
                        .background(LightGrayBackground, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = stringResource(R.string.select_date),
                        tint = Color.Black
                    )
                }
            }

            // Active filters row
            if (activeDate != null || searchQuery.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeDate != null) {
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        FilterChip(
                            selected = true,
                            onClick = { workoutHistoryViewModel.clearDateFilter() },
                            label = {
                                Text(
                                    text = dateFormat.format(activeDate!!),
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.clear_filter),
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Black,
                                labelColor = Color.White,
                                iconColor = Color.White,
                                selectedContainerColor = Color.Black
                            ),
                            border = null
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        FilterChip(
                            selected = true,
                            onClick = { workoutHistoryViewModel.updateSearchQuery("") },
                            label = {
                                Text(
                                    text = "\"$searchQuery\"",
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.clear_filter),
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Black,
                                labelColor = Color.White,
                                iconColor = Color.White,
                                selectedContainerColor = Color.Black
                            ),
                            border = null
                        )
                    }
                }
            }

            // Date picker dialog
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState()

                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                workoutHistoryViewModel.filterByDate(Date(millis))
                            }
                            showDatePicker = false
                        }) {
                            Text(stringResource(R.string.confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Workout history list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 0.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredWorkouts.isEmpty()) {
                    item {
                        NoWorkoutsFound()
                    }
                } else {
                    items(filteredWorkouts) { workout ->
                        WorkoutHistoryCard(
                            workout = workout,
                            onDetailsClick = {
                                // Pokaż szczegóły treningu w bottom sheet
                                selectedWorkoutId = workout.id
                            }
                        )
                    }
                }
            }

            // Wskaźnik ładowania przy pobieraniu szczegółów treningu
            if (isLoadingWorkout) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.Black)
                }
            }
        }
    }

    // Wyświetl bottom sheet jeśli jest wybrany trening
    selectedWorkout?.let { workout ->
        WorkoutDetailsBottomSheet(
            workout = workout,
            onDismiss = {
                selectedWorkout = null
                selectedWorkoutId = null
            },
            onWorkoutUpdate = { updatedWorkout ->
                // Użyj coroutineScope do wywołania funkcji suspend
                coroutineScope.launch {
                    try {
                        val result = workoutRepository.updateWorkout(updatedWorkout)
                        if (result.isSuccess) {
                            Log.d(TAG, "Workout updated successfully")
                            // Odświeżenie historii treningów po aktualizacji
                            currentUser?.let { user ->
                                workoutHistoryViewModel.loadWorkoutHistory(user.uid)
                            }
                        } else {
                            Log.e(TAG, "Failed to update workout: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception updating workout", e)
                    } finally {
                        selectedWorkout = null
                        selectedWorkoutId = null
                    }
                }
            }
        )
    }
}

@Composable
fun WorkoutHistoryCard(
    workout: CompletedWorkout,
    onDetailsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
            // Workout title and date
            Text(
                text = workout.name.ifEmpty { stringResource(R.string.unnamed_workout) },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            workout.endTime?.let { endTime ->
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                Text(
                    text = stringResource(R.string.completed_on, dateFormat.format(endTime.toDate())),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    text = stringResource(R.string.duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatDuration(workout.duration),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Exercises count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    text = stringResource(R.string.exercises_completed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${workout.exercises.size}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details button
            Button(
                onClick = onDetailsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray,
                    contentColor = Color.Black
                )
            ) {
                Text(text = stringResource(R.string.see_details))
            }

        }
    }
}

@Composable
fun NoWorkoutsFound() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.LightGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.no_workouts_found),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.try_different_search),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }
}

// Helper function to format duration
private fun formatDuration(seconds: Long): String {
    return TimeUtils.formatDurationSeconds(seconds)
}