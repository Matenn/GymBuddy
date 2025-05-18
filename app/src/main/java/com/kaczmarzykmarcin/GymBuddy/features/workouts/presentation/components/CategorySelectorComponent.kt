// CategorySelectorComponent.kt
package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kaczmarzykmarcin.GymBuddy.data.model.WorkoutCategory
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.viewmodel.WorkoutCategoryViewModel

@Composable
fun CategorySelector(
    modifier: Modifier = Modifier,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    viewModel: WorkoutCategoryViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val scrollState = rememberScrollState()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 8.dp)
        ) {
            // Przycisk "Wszystkie"
            CategoryChip(
                name = "Wszystkie",
                color = Color.Gray,
                isSelected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) }
            )

            // Lista kategorii
            categories.forEach { category ->
                CategoryChip(
                    name = category.name,
                    color = Color(android.graphics.Color.parseColor(category.color)),
                    isSelected = selectedCategoryId == category.id,
                    onClick = { onCategorySelected(category.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun CategoryChip(
    name: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) color else Color.Transparent)
            .border(
                width = 1.dp,
                color = color,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (isSelected) Color.White else color,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}