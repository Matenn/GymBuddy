package com.kaczmarzykmarcin.GymBuddy.features.statistics.presentation.components

import androidx.compose.foundation.layout.*
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
fun ExerciseStatistics(
    viewModel: StatisticsViewModel,
    modifier: Modifier = Modifier
) {
    val exerciseStats by viewModel.exerciseStatistics.collectAsState()

    exerciseStats?.let { stats ->
        Column(modifier = modifier.fillMaxWidth()) {
            // Personal Record Card
            PersonalRecordCard(
                weight = stats.personalBestFormatted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Statistics Cards Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.reps),
                        value = stats.personalBestReps.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = stringResource(R.string.average_weight),
                        value = stats.averageWeightFormatted,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.average_sets),
                        value = stats.averageSetsFormatted,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = stringResource(R.string.average_reps),
                        value = stats.averageReps.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Progress Chart
            SectionTitle(title = stringResource(R.string.progress_chart))

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
                    SingleExerciseProgressChart(
                        data = stats.progressPoints,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}