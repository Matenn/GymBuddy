
package com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model

/**
 * Pojedyncze źródło prawdy dla domyślnych definicji osiągnięć
 */
object DefaultAchievements {

    /**
     * Lista wszystkich domyślnych osiągnięć w systemie
     */
    val ALL = listOf(
        AchievementDefinition(
            id = "first_workout",
            title = "Pierwszy trening",
            description = "Wykonaj swój pierwszy trening",
            type = AchievementType.WORKOUT_COUNT,
            targetValue = 1,
            xpReward = 50,
            iconName = "🏃"
        ),
        AchievementDefinition(
            id = "morning_bird",
            title = "Poranny ptaszek",
            description = "Wykonaj 10 porannych treningów (przed 10:00)",
            type = AchievementType.MORNING_WORKOUTS,
            targetValue = 10,
            xpReward = 100,
            iconName = "🌅"
        ),
        AchievementDefinition(
            id = "workout_streak_3",
            title = "Mini seria",
            description = "Trenuj przez 3 dni z rzędu",
            type = AchievementType.WORKOUT_STREAK,
            targetValue = 3,
            xpReward = 100,
            iconName = "🔥"
        ),
        AchievementDefinition(
            id = "workout_streak_7",
            title = "Tygodniowa seria",
            description = "Trenuj przez 7 dni z rzędu",
            type = AchievementType.WORKOUT_STREAK,
            targetValue = 7,
            xpReward = 250,
            iconName = "💪"
        ),
        AchievementDefinition(
            id = "workout_count_10",
            title = "Regularny bywalec",
            description = "Ukończ 10 treningów",
            type = AchievementType.WORKOUT_COUNT,
            targetValue = 10,
            xpReward = 200,
            iconName = "⭐"
        ),
        AchievementDefinition(
            id = "workout_count_25",
            title = "Zaawansowany",
            description = "Ukończ 25 treningów",
            type = AchievementType.WORKOUT_COUNT,
            targetValue = 25,
            xpReward = 500,
            iconName = "🏆"
        ),
        AchievementDefinition(
            id = "workout_count_50",
            title = "Ekspert",
            description = "Ukończ 50 treningów",
            type = AchievementType.WORKOUT_COUNT,
            targetValue = 50,
            xpReward = 1000,
            iconName = "👑"
        ),
        AchievementDefinition(
            id = "workout_hour",
            title = "Godzinna sesja",
            description = "Zakończ trening trwający ponad godzinę",
            type = AchievementType.WORKOUT_DURATION,
            targetValue = 3600,
            xpReward = 150,
            iconName = "⏱️"
        ),
        AchievementDefinition(
            id = "workout_2_hours",
            title = "Maraton treningowy",
            description = "Zakończ trening trwający ponad 2 godziny",
            type = AchievementType.WORKOUT_DURATION,
            targetValue = 7200,
            xpReward = 300,
            iconName = "🏃‍♂️"
        ),
        AchievementDefinition(
            id = "bench_press_100kg",
            title = "Setka na ławce",
            description = "Wykonaj wyciskanie sztangi leżąc z obciążeniem 100kg",
            type = AchievementType.EXERCISE_WEIGHT,
            targetValue = 100,
            xpReward = 500,
            iconName = "💪",
            exerciseId = "Barbell_Bench_Press_-_Medium_Grip" // ID ćwiczenia z bazy
        )
    )

    /**
     * Pobiera definicję osiągnięcia po ID
     */
    fun getById(achievementId: String): AchievementDefinition? {
        return ALL.find { it.id == achievementId }
    }

    /**
     * Sprawdza czy osiągnięcie jest domyślne
     */
    fun isDefault(achievementId: String): Boolean {
        return ALL.any { it.id == achievementId }
    }
}