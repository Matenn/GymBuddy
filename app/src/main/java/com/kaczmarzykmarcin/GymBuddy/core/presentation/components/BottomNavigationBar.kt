// Create new file: com/kaczmarzykmarcin/GymBuddy/core/presentation/components/BottomNavigationBar.kt
package com.kaczmarzykmarcin.GymBuddy.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.navigation.NavigationRoutes

@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.White
    ) {
        // Dashboard
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.dashboard)) },
            selected = currentRoute == NavigationRoutes.MAIN,
            onClick = {
                if (currentRoute != NavigationRoutes.MAIN) {
                    navController.navigate(NavigationRoutes.MAIN) {
                        popUpTo(NavigationRoutes.MAIN) { inclusive = true }
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color.White
            )
        )

        // Workout History
        NavigationBarItem(
            icon = { Icon(Icons.Default.History, contentDescription = stringResource(R.string.history)) },
            selected = currentRoute == NavigationRoutes.HISTORY,
            onClick = {
                if (currentRoute != NavigationRoutes.HISTORY) {
                    navController.navigate(NavigationRoutes.HISTORY)
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color.White
            )
        )

        // Start Workout (center button)
        NavigationBarItem(
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.start_workout),
                        tint = Color.White
                    )
                }
            },
            selected = false,
            onClick = {
                navController.navigate(NavigationRoutes.WORKOUT_SCREEN)
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color.White
            )
        )

        // Exercise Library
        NavigationBarItem(
            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = stringResource(R.string.exercises)) },
            selected = currentRoute == NavigationRoutes.EXERCISES,
            onClick = {
                if (currentRoute != NavigationRoutes.EXERCISES) {
                    navController.navigate(NavigationRoutes.EXERCISES)
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color.White
            )
        )

        // Statistics
        NavigationBarItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.statistics)) },
            selected = currentRoute == NavigationRoutes.STATISTICS,
            onClick = {
                if (currentRoute != NavigationRoutes.STATISTICS) {
                    navController.navigate(NavigationRoutes.STATISTICS)
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color.White
            )
        )
    }
}