package com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kaczmarzykmarcin.GymBuddy.features.dashboard.presentation.BottomNavigationBar
import com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components.ExerciseLibraryContent

@Composable
fun ExerciseLibraryScreen(
    navController: NavController,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues())
    ) {
        ExerciseLibraryContent(
            viewModel = viewModel,
            selectionMode = false,
            showHeader = true
        )

        // Dolna nawigacja dla głównego ekranu
        BottomNavigationBar(
            navController = navController,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}