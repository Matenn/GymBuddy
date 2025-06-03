// BadgesScreen.kt
package com.kaczmarzykmarcin.GymBuddy.features.badges.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.kaczmarzykmarcin.GymBuddy.data.model.AchievementWithProgress
import com.kaczmarzykmarcin.GymBuddy.features.badges.presentation.viewmodel.BadgesViewModel
import com.kaczmarzykmarcin.GymBuddy.navigation.NavigationRoutes
import com.kaczmarzykmarcin.GymBuddy.features.badges.presentation.components.BadgeDetailBottomSheet


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(
    navController: NavController,
    badgesViewModel: BadgesViewModel = hiltViewModel()
) {
    val recentlyCompleted by badgesViewModel.recentlyCompleted.collectAsState()
    val inProgress by badgesViewModel.inProgress.collectAsState()
    val available by badgesViewModel.available.collectAsState()
    val userStats by badgesViewModel.userStats.collectAsState()
    val isLoading by badgesViewModel.isLoading.collectAsState()

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


    // Pobierz userId z FirebaseAuth
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            badgesViewModel.loadUserBadges(user.uid)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Odznaki") },
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
                    .verticalScroll(rememberScrollState())
            ) {
                // User Progress Header
                UserProgressHeader(userStats = userStats)

                Spacer(modifier = Modifier.height(24.dp))

                // Recently Completed Section
                if (recentlyCompleted.isNotEmpty()) {
                    BadgeSection(
                        title = "OSTATNIO ZDOBYTE",
                        badges = recentlyCompleted.take(3),
                        onSeeAllClick = {
                            navController.navigate("${NavigationRoutes.BADGES_DETAIL}/completed")
                        },
                        onBadgeClick = { badge ->
                            badgesViewModel.showBadgeDetails(badge)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // In Progress Section
                if (inProgress.isNotEmpty()) {
                    BadgeSection(
                        title = "W TRAKCIE REALIZACJI",
                        badges = inProgress.take(3),
                        onSeeAllClick = {
                            navController.navigate("${NavigationRoutes.BADGES_DETAIL}/in_progress")
                        },
                        onBadgeClick = { badge ->
                            badgesViewModel.showBadgeDetails(badge)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Available Section
                if (available.isNotEmpty()) {
                    BadgeSection(
                        title = "DO ZDOBYCIA",
                        badges = available.take(3),
                        onSeeAllClick = {
                            navController.navigate("${NavigationRoutes.BADGES_DETAIL}/available")
                        },
                        onBadgeClick = { badge ->
                            badgesViewModel.showBadgeDetails(badge)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun UserProgressHeader(
    userStats: BadgesViewModel.UserProgressStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Poziom ${userStats.currentLevel} • ${userStats.currentXP} XP",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar to next level
            LinearProgressIndicator(
                progress = { userStats.progressToNextLevel },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color.Black,
                trackColor = Color.LightGray,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${userStats.completedBadges}/${userStats.totalBadges} odznak zdobytych",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun BadgeSection(
    title: String,
    badges: List<AchievementWithProgress>,
    onSeeAllClick: () -> Unit,
    onBadgeClick: (AchievementWithProgress) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black
            )

            Text(
                text = "See all",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.Blue,
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            badges.forEach { badge ->
                BadgeCard(
                    badge = badge,
                    onClick = { onBadgeClick(badge) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Fill remaining space if less than 3 badges
            repeat(3 - badges.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun BadgeCard(
    badge: AchievementWithProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = badge.isCompleted
    val isLocked = badge.currentValue == 0 && !isCompleted

    Card(
        modifier = modifier
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
                    Spacer(modifier = Modifier.height(8.dp))
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