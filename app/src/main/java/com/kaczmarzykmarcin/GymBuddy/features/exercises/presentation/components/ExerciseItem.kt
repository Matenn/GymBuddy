package com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.features.exercises.domain.model.Exercise
import com.kaczmarzykmarcin.GymBuddy.core.presentation.theme.DarkGray

@Composable
fun ExerciseItem(
    exercise: Exercise,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = if (exercise.category.lowercase() == "cardio")
                    "Cardio"
                else exercise.primaryMuscles.firstOrNull()?.capitalizeFirstLetter() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkGray
            )
        }

        // Pokazujemy wskaźnik wyboru w trybie wyboru
        if (selectionMode) {
            if (isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = stringResource(R.string.selected),
                    tint = Color.Green,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.dp, Color.LightGray, CircleShape)
                )
            }
        }
    }
}

// Extension function to capitalize first letter
private fun String.capitalizeFirstLetter(): String {
    return if (this.isEmpty()) this else this.substring(0, 1).uppercase() + this.substring(1)
}