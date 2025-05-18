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
    const val EXERCISES = "exercises"
    const val STATISTICS = "statistics"
    const val PROFILE = "profile"

    // Workout routes
    const val START_WORKOUT = "start_workout"
    const val WORKOUT_SCREEN = "workout_screen"
    const val WORKOUT_DETAILS = "workout_details/{workoutId}"
    const val EXERCISE_DETAILS = "exercise_details/{exerciseId}"
    const val CREATE_WORKOUT_TEMPLATE = "create_workout_template"
    const val EDIT_WORKOUT_TEMPLATE = "edit_workout_template/{templateId}"
    const val CATEGORY_MANAGEMENT = "category_management"
}