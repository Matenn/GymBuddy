package com.kaczmarzykmarcin.GymBuddy.features.statistics.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.features.statistics.presentation.viewmodel.StatisticsViewModel

@Composable
fun ExerciseStatistics(
    viewModel: StatisticsViewModel,
    modifier: Modifier = Modifier
) {
    val exerciseStats by viewModel.exerciseStatistics.collectAsState()
    val selectedProgressMetric by viewModel.selectedProgressMetric.collectAsState()

    exerciseStats?.let { stats ->
        Column(modifier = modifier.fillMaxWidth()) {
            // Personal Records Row - Max Weight and Max 1RM (ze wszystkich czasów)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PersonalRecordCard(
                    weight = stats.personalBestFormatted,
                    title = stringResource(R.string.personal_record),
                    modifier = Modifier.weight(1f)
                )

                // Custom Max 1RM card matching PersonalRecordCard structure
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Max 1RM",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = "(ze wszystkich czasów)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = stats.personalBest1RMFormatted,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))




            // Progress Chart - używamy nowej wersji
            SingleExerciseProgressChart(
                data = stats.progressPoints,
                selectedMetric = selectedProgressMetric,
                onMetricSelected = viewModel::selectProgressMetric,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Całkowite reps", // ZMIANA: nowa etykieta
                        value = stats.totalRepsFormatted, // ZMIANA: używamy total reps
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

        }
    }
}