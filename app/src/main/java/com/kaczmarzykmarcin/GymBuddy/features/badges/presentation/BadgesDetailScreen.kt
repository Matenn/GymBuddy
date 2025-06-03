// BadgesDetailScreen.kt
package com.kaczmarzykmarcin.GymBuddy.features.badges.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.kaczmarzykmarcin.GymBuddy.data.model.AchievementType
import com.kaczmarzykmarcin.GymBuddy.data.model.AchievementWithProgress
import com.kaczmarzykmarcin.GymBuddy.features.badges.presentation.components.BadgeDetailBottomSheet
import com.kaczmarzykmarcin.GymBuddy.features.badges.presentation.viewmodel.BadgesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesDetailScreen(
    navController: NavController,
    category: String, // "completed", "in_progress", "available"
    badgesViewModel: BadgesViewModel = hiltViewModel()
) {
    val filteredBadges by badgesViewModel.filteredBadges.collectAsState()
    val selectedFilter by badgesViewModel.selectedCategoryFilter.collectAsState()
    val isLoading by badgesViewModel.isLoading.collectAsState()

    // Get title based on category
    val screenTitle = when (category) {
        "completed" -> "Zdobyte Odznaki"
        "in_progress" -> "W Trakcie"
        "available" -> "Do Zdobycia"
        else -> "Odznaki"
    }


    var selectedBadge by remember { mutableStateOf<AchievementWithProgress?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    // Obsługa bottom sheet
    LaunchedEffect(Unit) {
        badgesViewModel.showBadgeDetails.collect { badge ->
            selectedBadge = badge
            showBottomSheet = true
        }
    }

    // Bottom Sheet
    if (showBottomSheet && selectedBadge != null) {
        BadgeDetailBottomSheet(
            badge = selectedBadge!!,
            onDismiss = {
                showBottomSheet = false
                selectedBadge = null
            },
            onShare = {
                // TODO: Implementacja udostępniania
                showBottomSheet = false
                selectedBadge = null
            }
        )
    }


    // Load data on screen entry
    LaunchedEffect(category) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            badgesViewModel.loadDetailBadges(user.uid, category)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.Black)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues())
                    .padding(16.dp)
            ) {
                // Category Filter Pills
                CategoryFilterSection(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { filter ->
                        badgesViewModel.setCategoryFilter(filter)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Badges Grid
                if (filteredBadges.isEmpty()) {
                    EmptyBadgesState(category = category)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredBadges) { badge ->
                            BadgeDetailCard(
                                badge = badge,
                                onClick = {
                                    badgesViewModel.showBadgeDetails(badge)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterSection(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf(
        "all" to "Wszystkie",
        "workout_count" to "Treningi",
        "workout_streak" to "Serie",
        "exercise_weight" to "Ciężary",
        "workout_duration" to "Czas",
        "morning_workouts" to "Specjalne"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { (key, label) ->
                val isSelected = selectedFilter == key

                FilterChip(
                    onClick = { onFilterSelected(key) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = isSelected,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Black,
                        selectedLabelColor = Color.White,
                        containerColor = Color.Transparent,
                        labelColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun BadgeDetailCard(
    badge: AchievementWithProgress,
    onClick: () -> Unit
) {
    val isCompleted = badge.isCompleted
    val isLocked = badge.currentValue == 0 && !isCompleted

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> Color(0xFFFFD700) // Golden for completed
                isLocked -> Color(0xFFF5F5F5) // Light gray for locked
                else -> Color.White // White for in progress
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> Color.White.copy(alpha = 0.2f)
                                isLocked -> Color.Gray.copy(alpha = 0.2f)
                                else -> Color.Black.copy(alpha = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge.definition.iconName,
                        fontSize = 24.sp,
                        color = when {
                            isCompleted -> Color.White
                            isLocked -> Color.Gray
                            else -> Color.Black
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = badge.definition.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = when {
                        isCompleted -> Color.White
                        isLocked -> Color.Gray
                        else -> Color.Black
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Progress
                if (!isCompleted) {
                    Text(
                        text = "${badge.currentValue}/${badge.definition.targetValue}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLocked) Color.Gray else Color.Gray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { badge.progressPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (isLocked) Color.Gray else Color(0xFF2196F3),
                        trackColor = Color.Gray.copy(alpha = 0.3f),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    // Show completion date for completed badges
                    badge.progress?.completedAt?.let { completedAt ->
                        Text(
                            text = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                                .format(completedAt.toDate()),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // XP Reward
                Text(
                    text = "+${badge.definition.xpReward} XP",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = when {
                        isCompleted -> Color.White.copy(alpha = 0.9f)
                        isLocked -> Color.Gray
                        else -> Color.Black.copy(alpha = 0.7f)
                    }
                )
            }

            // Checkmark for completed badges
            if (isCompleted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyBadgesState(category: String) {
    val message = when (category) {
        "completed" -> "Brak zdobytych odznak w tej kategorii"
        "in_progress" -> "Brak odznak w trakcie realizacji"
        "available" -> "Brak dostępnych odznak w tej kategorii"
        else -> "Brak odznak"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🏆",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Wykonaj więcej treningów, aby zdobyć nowe odznaki!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}