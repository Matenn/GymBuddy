package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.data.model.WorkoutTemplate
import com.kaczmarzykmarcin.GymBuddy.features.dashboard.presentation.BottomNavigationBar
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.components.ActiveWorkoutMiniBar
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.components.TrainingRecorderBottomSheet
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.viewmodel.WorkoutViewModel
import com.kaczmarzykmarcin.GymBuddy.navigation.NavigationRoutes

@Composable
fun WorkoutScreen(
    navController: NavController,
    workoutViewModel: WorkoutViewModel = hiltViewModel()
) {
    val workoutTemplates by workoutViewModel.workoutTemplates.collectAsState()
    val activeWorkout by workoutViewModel.activeWorkout.collectAsState()
    val currentUserId by workoutViewModel.currentUserId.collectAsState()

    var showWorkoutRecorder by remember { mutableStateOf(false) }

    // Fetch user's workout templates when screen is first displayed
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            workoutViewModel.loadWorkoutTemplates(currentUserId)
            workoutViewModel.checkActiveWorkout(currentUserId)
        }
    }

    // When active workout changes (including when it becomes null), update the UI
    LaunchedEffect(activeWorkout) {
        // If active workout becomes null, ensure the bottom sheet is hidden
        if (activeWorkout == null) {
            showWorkoutRecorder = false
        }
    }

    // Calculate bottom nav height (constant value or using WindowInsets)
    val bottomNavHeight = 86.dp

    // Safe area at the bottom of the screen
    val bottomInsets = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues())
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.training),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(8.dp)
            )

            // Search field (not functional in this implementation)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEFF1F5)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.search_workout_placeholder),
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick start section
            Text(
                text = stringResource(R.string.quick_start),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(8.dp)
            )

            Button(
                onClick = {
                    // Only start a new workout if there isn't already an active one
                    if (activeWorkout == null) {
                        workoutViewModel.startNewWorkout(currentUserId)
                    }
                    showWorkoutRecorder = true
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
                        stringResource(R.string.resume_workout)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Planned workouts section
            Text(
                text = stringResource(R.string.planned_workouts),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(8.dp)
            )

            // Button to create a new template
            Button(
                onClick = { /* Navigate to template creation screen */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEFF1F5),
                    contentColor = Color.Black
                )
            ) {
                Text(text = stringResource(R.string.create_new_template))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of workout templates
            workoutTemplates.forEach { template ->
                WorkoutTemplateItem(
                    template = template,
                    onEditClick = { /* Navigate to template editing */ },
                    onTemplateClick = { /* Start workout with this template */ }
                )
            }

            // Add space for the bottom navigation bar
            Spacer(modifier = Modifier.height(80.dp))

            // Add extra space if there's an active workout (for minibar)
            if (activeWorkout != null && !showWorkoutRecorder && activeWorkout?.endTime == null) {
                Spacer(modifier = Modifier.height(56.dp)) // Height of minibar
            }
        }

        // If a workout is active and hasn't been canceled/finished, show the mini bar
        if (activeWorkout != null && !showWorkoutRecorder && activeWorkout?.endTime == null) {
            ActiveWorkoutMiniBar(
                workout = activeWorkout!!,
                onMiniBarClick = { showWorkoutRecorder = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomNavHeight + bottomInsets) // Accounts for navigation height and safe area
            )
        }

        // Bottom sheet for recording workout
        if (showWorkoutRecorder && activeWorkout != null) {
            TrainingRecorderBottomSheet(
                workout = activeWorkout!!,
                onDismiss = { showWorkoutRecorder = false },
                onWorkoutFinish = {
                    workoutViewModel.finishWorkout(it)
                    showWorkoutRecorder = false // Ensure sheet is hidden after finishing
                },
                onWorkoutCancel = {
                    workoutViewModel.cancelWorkout(it.id)
                    showWorkoutRecorder = false // Ensure sheet is hidden after canceling
                }
            )
        }

        // Bottom Navigation Bar
        BottomNavigationBar(
            navController = navController,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun WorkoutTemplateItem(
    template: WorkoutTemplate,
    onEditClick: () -> Unit,
    onTemplateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onTemplateClick() },
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_template)
                )
            }
        }
    }
}