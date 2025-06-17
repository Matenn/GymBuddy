package com.kaczmarzykmarcin.GymBuddy.features.statistics.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.core.presentation.components.AppScaffold
import com.kaczmarzykmarcin.GymBuddy.features.statistics.data.model.StatType
import com.kaczmarzykmarcin.GymBuddy.features.statistics.presentation.components.*
import com.kaczmarzykmarcin.GymBuddy.features.statistics.presentation.viewmodel.StatisticsViewModel
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.viewmodel.WorkoutViewModel

@Composable
fun StatisticsScreen(
    navController: NavController,
    viewModel: StatisticsViewModel = hiltViewModel(),
    workoutViewModel: WorkoutViewModel = hiltViewModel()
) {
    val selectedTimePeriod by viewModel.selectedTimePeriod.collectAsState()
    val selectedStatType by viewModel.selectedStatType.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedExercise by viewModel.selectedExercise.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    AppScaffold(
        navController = navController,
        workoutViewModel = workoutViewModel,
        contentPadding = PaddingValues(bottom = 80.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Fixed Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp, 0.dp, 16.dp, 0.dp)
                    .padding(
                        WindowInsets.safeDrawing
                            .only(WindowInsetsSides.Top)
                            .asPaddingValues()
                    )
            ) {
                // Header
                Text(
                    text = stringResource(R.string.statistics_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                    ),
                    modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 24.dp)
                )

                // Time Period Selector
                TimePeriodSelector(
                    selectedPeriod = selectedTimePeriod,
                    onPeriodSelected = viewModel::selectTimePeriod
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category/Exercise Type Selector
                StatTypeSelector(
                    selectedType = selectedStatType,
                    onTypeSelected = viewModel::selectStatType
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category/Exercise Selector
                when (selectedStatType) {
                    StatType.CATEGORY -> {
                        CategorySelector(
                            categories = allCategories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = viewModel::selectCategory
                        )
                    }
                    StatType.EXERCISE -> {
                        ExerciseSelector(
                            selectedExercise = selectedExercise,
                            onExerciseSelected = viewModel::selectExercise
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Scrollable Statistics Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp, 0.dp, 16.dp, 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Statistics Content
                when (selectedStatType) {
                    StatType.CATEGORY -> {
                        if (selectedCategory == null) {
                            AllCategoriesStatistics(viewModel = viewModel)
                        } else {
                            CategoryStatistics(viewModel = viewModel)
                        }
                    }
                    StatType.EXERCISE -> {
                        if (selectedExercise != null) {
                            ExerciseStatistics(viewModel = viewModel)
                        }
                    }
                }

                // Bottom spacing for navigation and potential mini bar
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}