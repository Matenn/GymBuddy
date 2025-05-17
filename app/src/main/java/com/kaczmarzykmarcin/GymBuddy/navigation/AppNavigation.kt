package com.kaczmarzykmarcin.GymBuddy.navigation

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.AuthState
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.AuthViewModel
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.components.LoadingIndicator
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.login.LoginScreen
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.register.RegisterScreen
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.reset.PasswordResetScreen
import com.kaczmarzykmarcin.GymBuddy.features.auth.presentation.welcome.WelcomeScreen
import com.kaczmarzykmarcin.GymBuddy.features.dashboard.presentation.DashboardScreen
import com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation.ExerciseLibraryScreen
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.WorkoutScreen
import com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.history.WorkoutHistoryScreen

private const val TAG = "AppNavigation"

@Composable
fun AppNavigation(navController: NavHostController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    // Debug logging
    LaunchedEffect(authState) {
        Log.d(TAG, "Auth state changed: $authState")
    }

    // Effect to handle navigation based on auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // Navigate to main screen when authenticated
                Log.d(TAG, "User authenticated, navigating to main. Current destination: ${navController.currentDestination?.route}")
                if (navController.currentDestination?.route != "main") {
                    navController.navigate("main") {
                        // Clear back stack to prevent going back to auth screens
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.NotAuthenticated -> {
                // Navigate to welcome screen when not authenticated
                Log.d(TAG, "User not authenticated, navigating to welcome. Current destination: ${navController.currentDestination?.route}")
                if (navController.currentDestination?.route != "welcome" &&
                    navController.currentDestination?.route != "login" &&
                    navController.currentDestination?.route != "register") {
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {} // No navigation change for other states
        }
    }

    // Use remembered auth state to immediately determine the start screen
    // This ensures immediate startup without a loading screen
    val isUserLoggedIn = authViewModel.getRememberedAuthState()
    Log.d(TAG, "Initial remembered auth state: $isUserLoggedIn")

    // Define the navigation graph with proper transitions
    NavHost(
        navController = navController,
        startDestination = if (isUserLoggedIn) "main" else "welcome"
    ) {
        composable(
            route = "welcome",
            enterTransition = {
                // When returning to welcome screen from login/register
                slideInHorizontally(
                    initialOffsetX = { -it }, // Slide in from left
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                // When leaving welcome screen to login/register
                slideOutHorizontally(
                    targetOffsetX = { -it }, // Slide out to left
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            WelcomeScreen(navController, authViewModel)
        }

        composable(
            route = "login",
            enterTransition = {
                // When entering login screen from welcome
                slideInHorizontally(
                    initialOffsetX = { it }, // Slide in from right
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                // When leaving login screen back to welcome
                slideOutHorizontally(
                    targetOffsetX = { it }, // Slide out to right
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            LoginScreen(navController, authViewModel)
        }

        composable(
            route = "register",
            enterTransition = {
                // When entering register screen from welcome
                slideInHorizontally(
                    initialOffsetX = { it }, // Slide in from right
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                // When leaving register screen back to welcome
                slideOutHorizontally(
                    targetOffsetX = { it }, // Slide out to right
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            RegisterScreen(navController, authViewModel)
        }

        composable(
            route = "password_reset",
            enterTransition = {
                // When entering register screen from welcome
                slideInHorizontally(
                    initialOffsetX = { it }, // Slide in from right
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                // When leaving register screen back to welcome
                slideOutHorizontally(
                    targetOffsetX = { it }, // Slide out to right
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            PasswordResetScreen(navController, authViewModel)
        }

        composable(
            route = "main",
            enterTransition = {
                // When entering main screen
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                // When leaving main screen
                fadeOut(animationSpec = tween(300))
            }
        ) {
            Log.d(TAG, "Loading DashboardScreen")
            DashboardScreen(navController, authViewModel)
        }

        composable(
            route = NavigationRoutes.EXERCISES,
            enterTransition = {
                // When entering exercises screen
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                // When leaving exercises screen
                fadeOut(animationSpec = tween(300))
            }
        ) {
            Log.d(TAG, "Loading ExerciseLibraryScreen")
            ExerciseLibraryScreen(navController)
        }

        composable(
            route = NavigationRoutes.HISTORY,
            enterTransition = {
                // When entering history screen
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                // When leaving history screen
                fadeOut(animationSpec = tween(300))
            }
        ) {
            Log.d(TAG, "Loading WorkoutHistoryScreen")
            WorkoutHistoryScreen(navController, authViewModel)
        }

        composable(
            route = NavigationRoutes.WORKOUT_SCREEN,
            enterTransition = {
                // When entering workout screen
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                // When leaving workout screen
                fadeOut(animationSpec = tween(300))
            }
        ) {
            Log.d(TAG, "Loading WorkoutScreen")
            WorkoutScreen(navController)
        }


    }
}