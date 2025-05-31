package com.kaczmarzykmarcin.GymBuddy.features.dashboard.presentation

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.core.presentation.components.AppScaffold
import com.kaczmarzykmarcin.GymBuddy.data.model.CompletedWorkout
import com.kaczmarzykmarcin.GymBuddy.data.model.UserData
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.AuthState
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.AuthViewModel
import com.kaczmarzykmarcin.GymBuddy.features.dashboard.data.repository.DashboardViewModel
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.viewmodel.WorkoutViewModel
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.components.WorkoutDetailsBottomSheet
import com.kaczmarzykmarcin.GymBuddy.navigation.NavigationRoutes
import com.kaczmarzykmarcin.GymBuddy.utils.TimeUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "DashboardScreen"

/**
 * Punkt wejścia do wstrzykiwania repozytorium - taki sam jak w WorkoutHistoryScreen
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface WorkoutRepositoryEntryPoint {
    val workoutRepository: com.kaczmarzykmarcin.GymBuddy.data.repository.WorkoutRepository
}

@Composable
fun DashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    workoutViewModel: WorkoutViewModel = hiltViewModel()
) {
    // Pobierz repozytorium przez EntryPoint - tak samo jak w WorkoutHistoryScreen
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context,
        WorkoutRepositoryEntryPoint::class.java
    )
    val workoutRepository = entryPoint.workoutRepository

    val userData by dashboardViewModel.userData.collectAsState()
    val lastWorkout by dashboardViewModel.lastWorkout.collectAsState()
    val weeklyActivity by dashboardViewModel.weeklyActivity.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Zmienne stanu dla szczegółów treningu - tak samo jak w WorkoutHistoryScreen
    var selectedWorkoutId by remember { mutableStateOf<String?>(null) }
    var selectedWorkout by remember { mutableStateOf<CompletedWorkout?>(null) }
    var isLoadingWorkout by remember { mutableStateOf(false) }

    // Dodaj zmienne stanu dla aktywnego treningu i rejestratora treningu
    val activeWorkout by workoutViewModel.activeWorkout.collectAsState()
    val showWorkoutRecorder by workoutViewModel.showWorkoutRecorder.collectAsState()

    val currentUser = when (val authState = authViewModel.authState.collectAsState().value) {
        is AuthState.Authenticated -> authState.user
        else -> null
    }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            dashboardViewModel.loadUserData(user.uid)
            dashboardViewModel.loadLastWorkout(user.uid)
            dashboardViewModel.loadWeeklyActivity(user.uid)

            // Also check for active workout
            workoutViewModel.checkActiveWorkout(user.uid)

            // Załaduj dane potrzebne do treningu
            workoutViewModel.loadCategories()
            workoutViewModel.loadAllExercises()
        }
    }

    // Ładuj szczegóły treningu gdy zostanie wybrany - tak samo jak w WorkoutHistoryScreen
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
        contentPadding = PaddingValues(bottom = 80.dp) // Dodaj odpowiedni padding na dole
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues())
                .padding(16.dp, 0.dp, 16.dp, 16.dp)
                .padding(paddingValues) // Dodaj padding przekazany z AppScaffold
                .verticalScroll(rememberScrollState())
        ) {
            // Header with profile icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Icon (clickable to navigate to profile)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable { /* Navigate to profile screen */ }
                        .border(1.dp, Color.Gray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Add spacing between profile icon and text
                Spacer(modifier = Modifier.width(16.dp))

                // Column for greeting text
                Column {
                    userData?.let { user ->
                        Text(
                            text = "Cześć, ${user.profile.displayName.ifEmpty { "Imię" }}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Text(
                            text = "Czas na kolejny trening!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.Gray
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Training section
            Text(
                text = stringResource(R.string.trainings),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            // Weekly activity calendar
            WeeklyActivityCalendar(weeklyActivity)

            Spacer(modifier = Modifier.height(16.dp))

            // Last workout summary
            Text(
                text = stringResource(R.string.workout_summary),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            if (lastWorkout != null) {
                LastWorkoutSummary(
                    workout = lastWorkout!!,
                    onDetailsClick = {
                        // Pokaż szczegóły treningu w bottom sheet - tak samo jak w WorkoutHistoryScreen
                        selectedWorkoutId = lastWorkout!!.id
                    }
                )
            } else {
                NoWorkoutYet()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start new workout button - zaktualizowana implementacja
            Button(
                onClick = {
                    currentUser?.let { user ->
                        // Only start a new workout if there isn't already an active one
                        if (activeWorkout == null) {
                            workoutViewModel.startNewWorkout(user.uid)
                        }
                        // Always show the recorder (whether new or existing workout)
                        workoutViewModel.showWorkoutRecorder(true)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                )
            ) {
                Text(
                    text = if (activeWorkout == null)
                        stringResource(R.string.start_new_workout)
                    else
                        stringResource(R.string.resume_workout),
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress section
            Text(
                text = stringResource(R.string.progress),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            userData?.let { user ->
                LevelProgressBar(
                    currentXp = user.stats.xp,
                    maxXp = calculateMaxXp(user.stats.level),
                    level = user.stats.level
                )
            }

            Spacer(modifier = Modifier.height(80.dp)) // Space for bottom navigation bar
        }

        // Wskaźnik ładowania przy pobieraniu szczegółów treningu - tak samo jak w WorkoutHistoryScreen
        if (isLoadingWorkout) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.Black)
            }
        }
    }

    // Wyświetl bottom sheet jeśli jest wybrany trening - tak samo jak w WorkoutHistoryScreen
    selectedWorkout?.let { workout ->
        WorkoutDetailsBottomSheet(
            workout = workout,
            onDismiss = {
                selectedWorkout = null
                selectedWorkoutId = null
            },
            onWorkoutUpdate = { updatedWorkout ->
                // Use coroutineScope to call suspend functions
                coroutineScope.launch {
                    try {
                        val result = workoutRepository.updateWorkout(updatedWorkout)
                        if (result.isSuccess) {
                            Log.d(TAG, "Workout updated successfully")

                            // Update the selected workout with the new data
                            selectedWorkout = updatedWorkout

                            // Refresh the dashboard data after update
                            currentUser?.let { user ->
                                dashboardViewModel.loadLastWorkout(user.uid)
                                dashboardViewModel.loadWeeklyActivity(user.uid)
                            }
                        } else {
                            Log.e(TAG, "Failed to update workout: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception updating workout", e)
                    }
                }
            },
            navController = navController
        )
    }

    // Dodaj obserwowanie zmian w treningu aby odświeżyć dashboard
    LaunchedEffect(activeWorkout?.endTime) {
        // Gdy trening zostanie zakończony (endTime się zmieni), odśwież dane dashboard
        if (activeWorkout?.endTime != null) {
            currentUser?.let { user ->
                dashboardViewModel.loadLastWorkout(user.uid)
                dashboardViewModel.loadWeeklyActivity(user.uid)
                dashboardViewModel.loadUserData(user.uid) // Odśwież też dane użytkownika (XP, poziom)
            }
        }
    }
}

@Composable
fun WeeklyActivityCalendar(weeklyActivity: Map<String, Boolean>) {
    val daysOfWeek = listOf(
        stringResource(R.string.monday_short),
        stringResource(R.string.tuesday_short),
        stringResource(R.string.wednesday_short),
        stringResource(R.string.thursday_short),
        stringResource(R.string.friday_short),
        stringResource(R.string.saturday_short),
        stringResource(R.string.sunday_short)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEach { day ->
                val isActive = weeklyActivity[day] ?: false
                DayCircle(day = day, isActive = isActive)
            }
        }
    }
}

@Composable
fun DayCircle(day: String, isActive: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (isActive) Color(0xFF90EE90) else Color.LightGray,
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun LastWorkoutSummary(
    workout: CompletedWorkout,
    onDetailsClick: () -> Unit // Dodaj parametr callback
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
            // Workout icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji for workout type
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💪",
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Workout name and date
                Column {
                    Text(
                        text = workout.name.ifEmpty { "Trening nóg" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    workout.endTime?.let { endTime ->
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        Text(
                            text = "Wykonano: ${dateFormat.format(endTime.toDate())}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Gray
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Workout details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
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

            Spacer(modifier = Modifier.height(12.dp))

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
                Spacer(modifier = Modifier.width(8.dp))
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

            // Details button - teraz z działającym callback
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
fun NoWorkoutYet() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.LightGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.no_workouts),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.no_workouts_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun LevelProgressBar(currentXp: Int, maxXp: Int, level: Int) {
    val progress = currentXp.toFloat() / maxXp.toFloat()
    val remainingWorkouts = ((maxXp - currentXp) / 50f).toInt() // Assuming 50 XP per workout on average

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
            // Level and XP info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.level) + " $level",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "$currentXp / $maxXp XP",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = Color.Black,
                trackColor = Color.LightGray,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Remaining workouts info
            Text(
                text = stringResource(R.string.workouts_to_level, remainingWorkouts, level + 1),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Helper functions
private fun formatDuration(seconds: Long): String {
    return TimeUtils.formatDurationSeconds(seconds)
}

private fun calculateMaxXp(level: Int): Int {
    return 100 + (level - 1) * 50
}