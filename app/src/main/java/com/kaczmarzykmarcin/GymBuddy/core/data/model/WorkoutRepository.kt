package com.kaczmarzykmarcin.GymBuddy.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kaczmarzykmarcin.GymBuddy.data.model.CompletedWorkout
import com.kaczmarzykmarcin.GymBuddy.data.model.WorkoutTemplate
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repozytorium do zarządzania szablonami treningów i historią treningów.
 */
@Singleton
class WorkoutRepository @Inject constructor (
    private val firestore: FirebaseFirestore,
    private val exerciseRepository: ExerciseRepository
) {
    private val workoutTemplatesCollection = firestore.collection("workoutTemplates")
    private val completedWorkoutsCollection = firestore.collection("completedWorkouts")

    /**
     * Pobiera wszystkie szablony treningów użytkownika.
     */
    suspend fun getUserWorkoutTemplates(userId: String): Result<List<WorkoutTemplate>> {
        return try {
            val snapshot = workoutTemplatesCollection
                .whereEqualTo("userId", userId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val templates = snapshot.documents.map { doc ->
                WorkoutTemplate.fromMap(doc.id, doc.data ?: mapOf())
            }

            Result.success(templates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera szablon treningu po ID.
     */
    suspend fun getWorkoutTemplate(templateId: String): Result<WorkoutTemplate> {
        return try {
            val doc = workoutTemplatesCollection.document(templateId).get().await()

            if (doc.exists()) {
                val template = WorkoutTemplate.fromMap(doc.id, doc.data ?: mapOf())
                Result.success(template)
            } else {
                Result.failure(Exception("Workout template not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tworzy nowy szablon treningu.
     */
    suspend fun createWorkoutTemplate(template: WorkoutTemplate): Result<WorkoutTemplate> {
        return try {
            val docRef = workoutTemplatesCollection.document()
            val templateWithId = template.copy(id = docRef.id)
            docRef.set(templateWithId.toMap()).await()
            Result.success(templateWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje istniejący szablon treningu.
     */
    suspend fun updateWorkoutTemplate(template: WorkoutTemplate): Result<WorkoutTemplate> {
        return try {
            val updatedTemplate = template.copy(updatedAt = Timestamp.now())
            workoutTemplatesCollection.document(template.id).set(updatedTemplate.toMap()).await()
            Result.success(updatedTemplate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Usuwa szablon treningu.
     */
    suspend fun deleteWorkoutTemplate(templateId: String): Result<Unit> {
        return try {
            workoutTemplatesCollection.document(templateId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rozpoczyna nowy trening.
     */
    suspend fun startWorkout(workout: CompletedWorkout): Result<CompletedWorkout> {
        return try {
            val docRef = completedWorkoutsCollection.document()
            val workoutWithId = workout.copy(id = docRef.id)
            docRef.set(workoutWithId.toMap()).await()
            Result.success(workoutWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Kończy trening (aktualizuje endTime i duration).
     */
    suspend fun finishWorkout(workoutId: String): Result<CompletedWorkout> {
        return try {
            val workoutDoc = completedWorkoutsCollection.document(workoutId).get().await()

            if (!workoutDoc.exists()) {
                return Result.failure(Exception("Workout not found"))
            }

            val workout = CompletedWorkout.fromMap(workoutId, workoutDoc.data ?: mapOf())
            val endTime = Timestamp.now()
            val duration = (endTime.seconds - workout.startTime.seconds) / 60

            val updates = mapOf(
                "endTime" to endTime,
                "duration" to duration
            )

            completedWorkoutsCollection.document(workoutId).update(updates).await()

            // Pobierz zaktualizowany dokument
            val updatedDoc = completedWorkoutsCollection.document(workoutId).get().await()
            val updatedWorkout = CompletedWorkout.fromMap(workoutId, updatedDoc.data ?: mapOf())

            Result.success(updatedWorkout)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje trening w trakcie (np. dodaje ćwiczenia).
     */
    suspend fun updateWorkout(workout: CompletedWorkout): Result<CompletedWorkout> {
        return try {
            completedWorkoutsCollection.document(workout.id).set(workout.toMap()).await()
            Result.success(workout)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera historię treningów użytkownika.
     */
    suspend fun getUserWorkoutHistory(userId: String): Result<List<CompletedWorkout>> {
        return try {
            val snapshot = completedWorkoutsCollection
                .whereEqualTo("userId", userId)
                .whereNotEqualTo("endTime", null) // tylko zakończone treningi
                .orderBy("endTime", Query.Direction.DESCENDING)
                .get()
                .await()

            val workouts = snapshot.documents.map { doc ->
                CompletedWorkout.fromMap(doc.id, doc.data ?: mapOf())
            }

            Result.success(workouts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera aktualnie trwający trening użytkownika (jeśli istnieje).
     */
    suspend fun getActiveWorkout(userId: String): Result<CompletedWorkout?> {
        return try {
            val snapshot = completedWorkoutsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("endTime", null) // tylko niezakończone treningi
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents.first()
                val workout = CompletedWorkout.fromMap(doc.id, doc.data ?: mapOf())
                Result.success(workout)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Anuluje trwający trening.
     */
    suspend fun cancelWorkout(workoutId: String): Result<Unit> {
        return try {
            completedWorkoutsCollection.document(workoutId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera szczegóły zakończonego treningu.
     */
    suspend fun getCompletedWorkout(workoutId: String): Result<CompletedWorkout> {
        return try {
            val doc = completedWorkoutsCollection.document(workoutId).get().await()

            if (doc.exists()) {
                val workout = CompletedWorkout.fromMap(doc.id, doc.data ?: mapOf())
                Result.success(workout)
            } else {
                Result.failure(Exception("Workout not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obserwuje szablony treningów użytkownika.
     */
    fun observeUserWorkoutTemplates(userId: String): Flow<List<WorkoutTemplate>> = callbackFlow {
        val listenerRegistration = workoutTemplatesCollection
            .whereEqualTo("userId", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val templates = snapshot.documents.map { doc ->
                    WorkoutTemplate.fromMap(doc.id, doc.data ?: mapOf())
                }

                // Emituj wynik
                trySend(templates)
            }

        // Zamknij listener po zakończeniu flow
        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Obserwuje historię treningów użytkownika.
     */
    fun observeUserWorkoutHistory(userId: String): Flow<List<CompletedWorkout>> = callbackFlow {
        val listenerRegistration = completedWorkoutsCollection
            .whereEqualTo("userId", userId)
            .whereNotEqualTo("endTime", null)
            .orderBy("endTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val workouts = snapshot.documents.map { doc ->
                    CompletedWorkout.fromMap(doc.id, doc.data ?: mapOf())
                }

                // Emituj wynik
                trySend(workouts)
            }

        // Zamknij listener po zakończeniu flow
        awaitClose {
            listenerRegistration.remove()
        }
    }
}