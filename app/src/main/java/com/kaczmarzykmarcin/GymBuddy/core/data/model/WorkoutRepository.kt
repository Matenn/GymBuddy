package com.kaczmarzykmarcin.GymBuddy.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.kaczmarzykmarcin.GymBuddy.common.network.NetworkConnectivityManager
import com.kaczmarzykmarcin.GymBuddy.data.model.CompletedWorkout
import com.kaczmarzykmarcin.GymBuddy.data.model.WorkoutTemplate
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.WorkoutDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.WorkoutTemplateDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.mapper.UserMappers
import com.kaczmarzykmarcin.GymBuddy.features.user.data.remote.RemoteUserDataSource
import com.kaczmarzykmarcin.GymBuddy.features.user.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

/**
 * Repozytorium do zarządzania szablonami treningów i historią treningów.
 * Obsługuje zarówno lokalne jak i zdalne źródło danych.
 */
@Singleton
class WorkoutRepository @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutDao: WorkoutDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val remoteDataSource: RemoteUserDataSource,
    private val syncManager: SyncManager,
    private val networkManager: NetworkConnectivityManager,
    private val mappers: UserMappers
) {
    private val TAG = "WorkoutRepository"

    /**
     * Pobiera wszystkie szablony treningów użytkownika jako Flow z lokalnej bazy danych.
     */
    fun getUserWorkoutTemplates(userId: String): Flow<List<WorkoutTemplate>> {
        return workoutTemplateDao.getWorkoutTemplatesByUserId(userId)
            .map { entities -> entities.map { mappers.toModel(it) } }
    }

    /**
     * Pobiera szablon treningu po ID.
     */
    suspend fun getWorkoutTemplate(templateId: String): Result<WorkoutTemplate> {
        return try {
            // Próba pobrania z lokalnej bazy danych
            val templateEntity = workoutTemplateDao.getWorkoutTemplateById(templateId)

            if (templateEntity != null) {
                Log.d(TAG, "Retrieved workout template from local database")
                return Result.success(mappers.toModel(templateEntity))
            }

            // Jeśli nie ma danych lokalnie, pobierz z Firebase
            Log.d(TAG, "Template not found locally, trying Firebase")
            val remoteResult = remoteDataSource.getWorkoutTemplate(templateId)

            if (remoteResult.isSuccess) {
                val template = remoteResult.getOrNull()!!

                // Zapisz lokalnie
                workoutTemplateDao.insertWorkoutTemplate(mappers.toEntity(template))

                return Result.success(template)
            }

            remoteResult
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get workout template", e)
            Result.failure(e)
        }
    }

    /**
     * Tworzy nowy szablon treningu.
     */
    suspend fun createWorkoutTemplate(template: WorkoutTemplate): Result<WorkoutTemplate> {
        return try {
            // Generuj nowe ID jeśli nie zostało dostarczone
            val templateWithId = if (template.id.isEmpty()) {
                template.copy(id = UUID.randomUUID().toString())
            } else {
                template
            }

            // Zapisz lokalnie
            val entity = mappers.toEntity(templateWithId, true)
            workoutTemplateDao.insertWorkoutTemplate(entity)

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            Result.success(templateWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create workout template", e)
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje istniejący szablon treningu.
     */
    suspend fun updateWorkoutTemplate(template: WorkoutTemplate): Result<WorkoutTemplate> {
        return try {
            // Aktualizuj datę modyfikacji
            val updatedTemplate = template.copy(updatedAt = Timestamp.now())

            // Zapisz lokalnie
            val entity = mappers.toEntity(updatedTemplate, true)
            workoutTemplateDao.updateWorkoutTemplate(entity)

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            Result.success(updatedTemplate)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update workout template", e)
            Result.failure(e)
        }
    }

    /**
     * Usuwa szablon treningu.
     */
    suspend fun deleteWorkoutTemplate(templateId: String): Result<Unit> {
        return try {
            // Usuń lokalnie
            workoutTemplateDao.deleteWorkoutTemplate(templateId)

            // Usuń w Firebase - jeśli jest dostępny
            if (networkManager.isInternetAvailable()) {
                remoteDataSource.deleteWorkoutTemplate(templateId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete workout template", e)
            Result.failure(e)
        }
    }

    /**
     * Rozpoczyna nowy trening.
     */
    suspend fun startWorkout(workout: CompletedWorkout): Result<CompletedWorkout> {
        return try {
            // Generuj nowe ID jeśli nie zostało dostarczone
            val workoutWithId = if (workout.id.isEmpty()) {
                workout.copy(id = UUID.randomUUID().toString())
            } else {
                workout
            }

            // Zapisz lokalnie
            val entity = mappers.toEntity(workoutWithId, true)
            workoutDao.insertCompletedWorkout(entity)

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            Result.success(workoutWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start workout", e)
            Result.failure(e)
        }
    }

    /**
     * Kończy trening (aktualizuje endTime i duration).
     */
    suspend fun finishWorkout(workoutId: String): Result<CompletedWorkout> {
        return try {
            // Pobierz trening z lokalnej bazy danych
            val workoutEntity = workoutDao.getCompletedWorkoutById(workoutId)
            if (workoutEntity == null) {
                Log.e(TAG, "Workout not found for finishing")
                return Result.failure(Exception("Workout not found"))
            }

            // Konwertuj na model
            val workout = mappers.toModel(workoutEntity)

            // Ustaw czas zakończenia i oblicz czas trwania w sekundach
            val endTime = Timestamp.now()
            val duration = endTime.seconds - workout.startTime.seconds

            // Zaktualizuj model
            val updatedWorkout = workout.copy(
                endTime = endTime,
                duration = duration  // Teraz przechowuje sekundy
            )

            // Zapisz lokalnie
            val updatedEntity = mappers.toEntity(updatedWorkout, true)
            workoutDao.updateCompletedWorkout(updatedEntity)

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            Result.success(updatedWorkout)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finish workout", e)
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje trening w trakcie (np. dodaje ćwiczenia).
     */
    suspend fun updateWorkout(workout: CompletedWorkout): Result<CompletedWorkout> {
        return try {
            // Zapisz lokalnie
            val entity = mappers.toEntity(workout, true)
            workoutDao.updateCompletedWorkout(entity)

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            Result.success(workout)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update workout", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera historię treningów użytkownika jako Flow z lokalnej bazy danych.
     */
    fun getUserWorkoutHistory(userId: String): Flow<List<CompletedWorkout>> {
        return workoutDao.getCompletedWorkoutsByUserId(userId)
            .map { entities -> entities.map { mappers.toModel(it) } }
    }

    /**
     * Pobiera aktualnie trwający trening użytkownika (jeśli istnieje).
     */
    suspend fun getActiveWorkout(userId: String): Result<CompletedWorkout?> {
        return try {
            // Pobierz z lokalnej bazy danych
            val activeWorkoutEntity = workoutDao.getActiveWorkout(userId)

            if (activeWorkoutEntity != null) {
                return Result.success(mappers.toModel(activeWorkoutEntity))
            }

            // Jeśli nie ma danych lokalnie i jest sieć, pobierz z Firebase
            if (networkManager.isInternetAvailable()) {
                val remoteResult = remoteDataSource.getActiveWorkout(userId)

                if (remoteResult.isSuccess) {
                    val activeWorkout = remoteResult.getOrNull()

                    // Jeśli znaleziono aktywny trening, zapisz lokalnie
                    if (activeWorkout != null) {
                        workoutDao.insertCompletedWorkout(mappers.toEntity(activeWorkout))
                    }

                    return Result.success(activeWorkout)
                }
            }

            // Brak aktywnego treningu
            Result.success(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active workout", e)
            Result.failure(e)
        }
    }

    /**
     * Anuluje trwający trening.
     */
    suspend fun cancelWorkout(workoutId: String): Result<Unit> {
        return try {
            // Usuń lokalnie
            workoutDao.deleteCompletedWorkout(workoutId)

            // Usuń w Firebase - jeśli jest dostępny
            if (networkManager.isInternetAvailable()) {
                remoteDataSource.deleteWorkout(workoutId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel workout", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera szczegóły zakończonego treningu.
     */
    suspend fun getCompletedWorkout(workoutId: String): Result<CompletedWorkout> {
        return try {
            // Pobierz z lokalnej bazy danych
            val workoutEntity = workoutDao.getCompletedWorkoutById(workoutId)

            if (workoutEntity != null) {
                return Result.success(mappers.toModel(workoutEntity))
            }

            // Jeśli nie ma danych lokalnie, pobierz z Firebase
            if (networkManager.isInternetAvailable()) {
                val remoteResult = remoteDataSource.getCompletedWorkout(workoutId)

                if (remoteResult.isSuccess) {
                    val workout = remoteResult.getOrNull()!!

                    // Zapisz lokalnie
                    workoutDao.insertCompletedWorkout(mappers.toEntity(workout))

                    return Result.success(workout)
                }

                return remoteResult
            }

            Result.failure(Exception("Workout not found"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get completed workout", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera wszystkie szablony treningów użytkownika z Firebase i zapisuje lokalnie.
     */
    suspend fun syncWorkoutTemplates(userId: String): Result<List<WorkoutTemplate>> {
        return try {
            if (!networkManager.isInternetAvailable()) {
                return Result.failure(Exception("No network connection"))
            }

            val remoteResult = remoteDataSource.getUserWorkoutTemplates(userId)

            if (remoteResult.isSuccess) {
                val templates = remoteResult.getOrNull()!!

                // Zapisz wszystkie szablony lokalnie
                val entities = templates.map { mappers.toEntity(it) }
                entities.forEach { workoutTemplateDao.insertWorkoutTemplate(it) }

                Result.success(templates)
            } else {
                remoteResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync workout templates", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera historię treningów użytkownika z Firebase i zapisuje lokalnie.
     */
    suspend fun syncWorkoutHistory(userId: String): Result<List<CompletedWorkout>> {
        return try {
            if (!networkManager.isInternetAvailable()) {
                return Result.failure(Exception("No network connection"))
            }

            val remoteResult = remoteDataSource.getUserWorkoutHistory(userId)

            if (remoteResult.isSuccess) {
                val workouts = remoteResult.getOrNull()!!

                // Zapisz wszystkie treningi lokalnie
                val entities = workouts.map { mappers.toEntity(it) }
                entities.forEach { workoutDao.insertCompletedWorkout(it) }

                Result.success(workouts)
            } else {
                remoteResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync workout history", e)
            Result.failure(e)
        }
    }
}