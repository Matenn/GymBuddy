package com.kaczmarzykmarcin.GymBuddy.navigation

/**
 * Contains all navigation routes used in the application.
 */
object NavigationRoutes {
    // Auth routes
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PASSWORD_RESET = "password_reset"

    // Main navigation
    const val MAIN = "main"
    const val HISTORY = "history"
    const val START_WORKOUT = "start_workout"
    const val EXERCISES = "exercises"
    const val STATISTICS = "statistics"
    const val PROFILE = "profile"

    // Workout routes
    const val WORKOUT_DETAILS = "workout_details/{workoutId}"
    const val EXERCISE_DETAILS = "exercise_details/{exerciseId}"
}