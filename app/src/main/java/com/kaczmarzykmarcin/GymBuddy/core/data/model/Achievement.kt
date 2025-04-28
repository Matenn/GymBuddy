package com.kaczmarzykmarcin.GymBuddy.data.model

/**
 * Enum reprezentujący wszystkie możliwe osiągnięcia w aplikacji GymBuddy
 * na podstawie makiet.
 */
enum class Achievement(val id: Int, val title: String, val description: String, val xpReward: Int = 0) {
    // Osiągnięcia widoczne na makietach
    FIRST_WORKOUT(1, "Pierwszy trening", "Wykonaj swój pierwszy trening", 50),
    MORNING_BIRD(2, "Poranny ptaszek", "Wykonaj 10 porannych treningów", 100),

    // Dodatkowe osiągnięcia, które mogą być zaimplementowane
    WORKOUT_STREAK_3(3, "Mini seria", "Trenuj przez 3 dni z rzędu", 100),
    WORKOUT_STREAK_7(4, "Tygodniowa seria", "Trenuj przez 7 dni z rzędu", 250),
    WORKOUT_COUNT_10(5, "Regularny bywalec", "Ukończ 10 treningów", 200),
    BENCH_PRESS_MASTER(6, "Mistrz wyciskania", "Wykonaj bench press z obciążeniem powyżej 100kg", 300),
    WORKOUT_HOUR(7, "Godzinna sesja", "Zakończ trening trwający ponad godzinę", 150);

    companion object {
        /**
         * Zwraca osiągnięcie po jego identyfikatorze.
         */
        fun getById(id: Int): Achievement? = values().find { it.id == id }

        /**
         * Konwertuje listę ID na listę obiektów Achievement.
         */
        fun getByIds(ids: List<Int>): List<Achievement> = ids.mapNotNull { getById(it) }
    }
}