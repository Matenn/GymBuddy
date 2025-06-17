package com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaczmarzykmarcin.GymBuddy.features.exercises.domain.model.Exercise
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
        // Usuwamy padding na górze, który może powodować problemy z pozycjonowaniem
        modifier = Modifier
    ) {
        ExerciseLibraryContent(
            viewModel = viewModel,
            selectionMode = true,  // To ustawienie spowoduje użycie kompaktowego scrollbara
            showHeader = true,
            onBackPressed = { onDismiss() },
            onExercisesSelected = { selectedExercises ->
                onExercisesSelected(selectedExercises)
                onDismiss()
            }
        )
    }
}