// BadgeDetailBottomSheet.kt
package com.kaczmarzykmarcin.GymBuddy.features.achievements.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model.AchievementType
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model.AchievementWithProgress
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeDetailBottomSheet(
    badge: AchievementWithProgress,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    val isCompleted = badge.isCompleted
    val isLocked = badge.currentValue == 0 && !isCompleted

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.White,
        contentColor = Color.Black,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .size(40.dp, 4.dp)
                    .background(Color.LightGray, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Badge Icon (larger)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> Color(0xFFFFD700)
                            isLocked -> Color.LightGray
                            else -> Color(0xFF2196F3).copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.definition.iconName,
                    fontSize = 40.sp,
                    color = when {
                        isCompleted -> Color.White
                        isLocked -> Color.Gray
                        else -> Color(0xFF2196F3)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Badge Title
            Text(
                text = badge.definition.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Badge Description
            Text(
                text = badge.definition.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8F9FA)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isCompleted) {
                        // Completion info
                        Text(
                            text = "✅ Odznaka zdobyta!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF4CAF50)
                        )

                        badge.progress?.completedAt?.let { completedAt ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Zdobyta: ${SimpleDateFormat("dd MMMM yyyy", Locale("pl")).format(completedAt.toDate())}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else {
                        // Progress info
                        Text(
                            text = "Postęp",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "${badge.currentValue} / ${badge.definition.targetValue}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isLocked) Color.Gray else Color(0xFF2196F3)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { badge.progressPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isLocked) Color.Gray else Color(0xFF2196F3),
                            trackColor = Color.LightGray,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${String.format("%.1f", badge.progressPercentage)}% ukończone",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )

                        if (!isLocked) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pozostało: ${badge.remainingToComplete}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Additional Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(
                    label = "Nagroda",
                    value = "+${badge.definition.xpReward} XP"
                )

                InfoItem(
                    label = "Kategoria",
                    value = getAchievementTypeDisplayName(badge.definition.type)
                )

                InfoItem(
                    label = "Typ",
                    value = if (badge.definition.targetValue == 1) "Jednorazowe" else "Postępowe"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Share button (only for completed badges)
            if (isCompleted && onShare != null) {
                Button(
                    onClick = onShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Udostępnij osiągnięcie",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color.Black
        )
    }
}

private fun getAchievementTypeDisplayName(type: AchievementType): String {
    return when (type) {
        AchievementType.WORKOUT_COUNT -> "Treningi"
        AchievementType.WORKOUT_STREAK -> "Serie"
        AchievementType.MORNING_WORKOUTS -> "Specjalne"
        AchievementType.EXERCISE_WEIGHT -> "Ciężary"
        AchievementType.WORKOUT_DURATION -> "Czas"
        AchievementType.FIRST_TIME -> "Jednorazowe"
    }
}