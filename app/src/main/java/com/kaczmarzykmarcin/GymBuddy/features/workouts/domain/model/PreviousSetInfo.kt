package com.kaczmarzykmarcin.GymBuddy.features.workouts.domain.model

data class PreviousSetInfo(
    val setType: String,
    val normalSetNumber: Int, // Numer serii normalnej (1, 2, 3 itd.)
    val weight: Double,
    val reps: Int
)