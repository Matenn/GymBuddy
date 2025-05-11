package com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaczmarzykmarcin.GymBuddy.data.model.Exercise
import com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.ExerciseLibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectionBottomSheet(
    onDismiss: () -> Unit,
    onExercisesSelected: (List<Exercise>) -> Unit,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = Modifier.padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues())
    ) {
        ExerciseLibraryContent(
            viewModel = viewModel,
            selectionMode = true,
            showHeader = true,
            onBackPressed = { onDismiss() },
            onExercisesSelected = { selectedExercises ->
                onExercisesSelected(selectedExercises)
                onDismiss()
            }
        )
    }
}