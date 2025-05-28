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
    val basicStats by viewModel.basicStats.collectAsState()
    // ZMIANA: używamy reaktywnego StateFlow zamiast funkcji
    val progressData by viewModel.progressData.collectAsState()
    val exerciseDistribution by viewModel.exerciseDistribution.collectAsState()
    val selectedExercisesForChart by viewModel.selectedExercisesForChart.collectAsState()
    val showAllExercisesInChart by viewModel.showAllExercisesInChart.collectAsState()
    val filteredWorkouts by viewModel.filteredWorkouts.collectAsState()
    val selectedProgressMetric by viewModel.selectedProgressMetric.collectAsState()

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

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Chart Section with all elements in one card
        ProgressLineChart(
            data = progressData, // ZMIANA: używamy reaktywnego StateFlow
            selectedExercisesForChart = selectedExercisesForChart,
            showAllExercisesInChart = showAllExercisesInChart,
            availableExercises = availableExercises,
            onToggleExerciseForChart = viewModel::toggleExerciseForChart,
            onToggleShowAllExercisesInChart = viewModel::toggleShowAllExercisesInChart,
            modifier = Modifier.fillMaxWidth(),
            selectedMetric = selectedProgressMetric,
            onMetricSelected = viewModel::selectProgressMetric,
        )

        // Exercise Distribution Chart
        if (exerciseDistribution.isNotEmpty()) {


            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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