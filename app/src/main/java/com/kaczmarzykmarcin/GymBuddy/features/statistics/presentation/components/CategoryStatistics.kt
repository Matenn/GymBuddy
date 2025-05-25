package com.kaczmarzykmarcin.GymBuddy.features.statistics.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.features.statistics.presentation.viewmodel.StatisticsViewModel

@Composable
fun CategoryStatistics(
    viewModel: StatisticsViewModel,
    modifier: Modifier = Modifier
) {
    val basicStats = viewModel.calculateBasicStats()
    val progressData = viewModel.calculateProgressData()
    val exerciseDistribution = viewModel.calculateExerciseDistribution()
    val selectedExercisesForChart by viewModel.selectedExercisesForChart.collectAsState()
    val showAllExercisesInChart by viewModel.showAllExercisesInChart.collectAsState()
    val filteredWorkouts by viewModel.filteredWorkouts.collectAsState()

    // Get all unique exercises from filtered workouts
    val availableExercises = remember(filteredWorkouts) {
        filteredWorkouts
            .flatMap { it.exercises }
            .distinctBy { it.exerciseId }
            .map { it.exerciseId to it.name }
            .toMap()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Basic Statistics Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = stringResource(R.string.workouts),
                value = basicStats.totalWorkouts.toString(),
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = stringResource(R.string.total_time),
                value = basicStats.totalTimeFormatted,
                modifier = Modifier.weight(1f)
            )
        }

        // Progress Chart Section
        SectionTitle(title = stringResource(R.string.progress_chart))

        // Exercise filter chips
        if (availableExercises.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // "All" chip
                item {
                    ExerciseChipFilter(
                        exerciseId = "all",
                        exerciseName = stringResource(R.string.all),
                        isSelected = showAllExercisesInChart,
                        onToggle = { viewModel.toggleShowAllExercisesInChart() }
                    )
                }

                // Individual exercise chips
                items(availableExercises.toList()) { (exerciseId, exerciseName) ->
                    ExerciseChipFilter(
                        exerciseId = exerciseId,
                        exerciseName = exerciseName,
                        isSelected = selectedExercisesForChart.contains(exerciseId),
                        onToggle = { viewModel.toggleExerciseForChart(exerciseId) }
                    )
                }
            }
        }

        // Progress Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(16.dp)
            ) {
                ProgressLineChart(
                    data = progressData,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Exercise Distribution Chart
        if (exerciseDistribution.isNotEmpty()) {
            SectionTitle(title = stringResource(R.string.exercise_distribution))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp)
                ) {
                    ExercisePieChart(
                        data = exerciseDistribution,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}