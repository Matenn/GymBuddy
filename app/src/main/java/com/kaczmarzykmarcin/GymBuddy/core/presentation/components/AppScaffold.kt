// Create new file: com/kaczmarzykmarcin/GymBuddy/core/presentation/components/AppScaffold.kt
package com.kaczmarzykmarcin.GymBuddy.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.components.ActiveWorkoutMiniBar
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.components.TrainingRecorderBottomSheet
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.viewmodel.WorkoutViewModel
import com.kaczmarzykmarcin.GymBuddy.core.navigation.NavigationRoutes

// Modify: com/kaczmarzykmarcin/GymBuddy/core/presentation/components/AppScaffold.kt

@Composable
fun AppScaffold(
    navController: NavController,
    workoutViewModel: WorkoutViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable (PaddingValues) -> Unit
) {
    val activeWorkout by workoutViewModel.activeWorkout.collectAsState()
    val showWorkoutRecorder by workoutViewModel.showWorkoutRecorder.collectAsState()

    // Calculate bottom nav height
    val bottomNavHeight = 80.dp

    // Safe area at the bottom of the screen
    val bottomInsets = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        content(contentPadding)

        // Show the mini bar only if there's an active workout, it's not ended, and the recorder isn't showing
        if (activeWorkout != null && !showWorkoutRecorder && activeWorkout?.endTime == null) {
            ActiveWorkoutMiniBar(
                workout = activeWorkout!!,
                onMiniBarClick = {
                    // Just show the recorder without navigating
                    workoutViewModel.showWorkoutRecorder(true)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomNavHeight + bottomInsets) // Accounts for navigation height and safe area
            )
        }

        // Bottom sheet for recording workout
        if (showWorkoutRecorder && activeWorkout != null) {
            TrainingRecorderBottomSheet(
                workout = activeWorkout!!,
                onDismiss = { workoutViewModel.showWorkoutRecorder(false) },
                onWorkoutFinish = {
                    workoutViewModel.finishWorkout(it)
                    workoutViewModel.showWorkoutRecorder(false)
                },
                onWorkoutCancel = {
                    workoutViewModel.cancelWorkout(it.id)
                    workoutViewModel.showWorkoutRecorder(false)
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

// Add to AppScaffold.kt or create a new utilities file
@Composable
fun rememberContentPadding(
    workoutViewModel: WorkoutViewModel = hiltViewModel()
): PaddingValues {
    val activeWorkout by workoutViewModel.activeWorkout.collectAsState()
    val showWorkoutRecorder by workoutViewModel.showWorkoutRecorder.collectAsState()

    // Base padding from safe area
    val safeAreaPadding = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        .asPaddingValues()

    // Calculate bottom padding
    val bottomNavHeight = 80.dp
    val miniBarHeight = 56.dp

    // If there's an active workout and the recorder isn't showing, add extra padding for the minibar
    val bottomPadding = if (activeWorkout != null &&
        !showWorkoutRecorder &&
        activeWorkout?.endTime == null) {
        bottomNavHeight + miniBarHeight
    } else {
        bottomNavHeight
    }

    return PaddingValues(
        start = safeAreaPadding.calculateStartPadding(LocalLayoutDirection.current) + 16.dp,
        top = safeAreaPadding.calculateTopPadding(),
        end = safeAreaPadding.calculateEndPadding(LocalLayoutDirection.current) + 16.dp,
        bottom = bottomPadding + 16.dp // Adding some extra spacing
    )
}