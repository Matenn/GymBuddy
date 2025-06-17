package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kaczmarzykmarcin.GymBuddy.features.workouts.domain.model.CompletedWorkout
import com.kaczmarzykmarcin.GymBuddy.core.utils.TimeUtils
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutMiniBar(
    workout: CompletedWorkout,
    onMiniBarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Add a check for workout.endTime to prevent showing the minibar for canceled workouts
    if (workout.endTime != null) {
        return // Don't show the minibar if the workout has ended or been canceled
    }

    // Added timeState to force recomposition
    var timeState by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Format the elapsed time
    val elapsedTime = remember(workout, timeState) {
        val currentTime = System.currentTimeMillis()
        val startTime = workout.startTime.toDate().time
        val elapsedMillis = currentTime - startTime
        TimeUtils.formatElapsedTime(elapsedMillis)
    }

    // Properly clean up the timer when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            // Clean up timer resources when composable is disposed
        }
    }

    // Update time every second with a safer implementation
    LaunchedEffect(workout.id) { // Use workout.id as key to restart effect if workout changes
        while(workout.endTime == null) { // Only run timer if workout hasn't ended
            delay(1000)
            timeState = System.currentTimeMillis() // Trigger recomposition
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onMiniBarClick() },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = workout.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = elapsedTime,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )

            // Display exercise count if available
            if (workout.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${workout.exercises.size} exercises",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}