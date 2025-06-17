package com.kaczmarzykmarcin.GymBuddy.core.utils

import android.content.Context
import android.util.Log

object AssetDebugHelper {
    private const val TAG = "AssetDebugHelper"

    fun debugCompleteAssetsStructure(context: Context) {
        Log.d(TAG, "🔍 === PEŁNE DEBUGOWANIE STRUKTURY ASSETS ===")

        // Sprawdź root assets
        listAssetsRecursively(context, "")
    }

    private fun listAssetsRecursively(context: Context, path: String, level: Int = 0) {
        val indent = "  ".repeat(level)

        try {
            val items = context.assets.list(path) ?: return

            if (items.isEmpty()) {
                Log.d(TAG, "$indent📄 $path (plik)")
                return
            }

            if (path.isNotEmpty()) {
                Log.d(TAG, "$indent📁 $path/")
            }

            items.forEach { item ->
                val fullPath = if (path.isEmpty()) item else "$path/$item"
                listAssetsRecursively(context, fullPath, level + 1)
            }

        } catch (e: Exception) {
            Log.e(TAG, "$indent❌ Błąd czytania '$path': ${e.message}")
        }
    }

    fun verifyExerciseImages(context: Context, exerciseName: String) {
        Log.d(TAG, "🎯 === WERYFIKACJA ZDJĘĆ DLA: '$exerciseName' ===")

        // Lista możliwych wariantów nazwy
        val nameVariants = generateNameVariants(exerciseName)

        nameVariants.forEach { variant ->
            Log.d(TAG, "🔍 Sprawdzam wariant: '$variant'")

            val folderPath = "exercise_image/$variant"

            // Sprawdź czy folder istnieje
            val folderExists = try {
                val items = context.assets.list(folderPath)
                items != null && items.isNotEmpty()
            } catch (e: Exception) {
                false
            }

            if (folderExists) {
                Log.d(TAG, "✅ Folder istnieje: $folderPath")

                // Lista plików w folderze
                try {
                    val files = context.assets.list(folderPath)
                    Log.d(TAG, "📋 Pliki w folderze: ${files?.joinToString(", ")}")

                    files?.forEach { file ->
                        val fullPath = "$folderPath/$file"
                        val fileExists = checkFileExists(context, fullPath)
                        Log.d(TAG, "   ${if (fileExists) "✅" else "❌"} $file")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Błąd listowania plików w $folderPath: ${e.message}")
                }
            } else {
                Log.d(TAG, "❌ Folder nie istnieje: $folderPath")
            }
        }
    }

    private fun generateNameVariants(name: String): List<String> {
        return listOf(
            name,                                          // "Push Up"
            name.lowercase(),                              // "push up"
            name.lowercase().replace(" ", "_"),            // "push_up"
            name.lowercase().replace(" ", "-"),            // "push-up"
            name.replace(" ", "_"),                        // "Push_Up"
            name.replace(" ", "-"),                        // "Push-Up"
            name.lowercase().replace(Regex("[^a-z0-9]"), "_"), // "push_up" (usuwa wszystkie znaki specjalne)
            name.lowercase().replace(Regex("\\s+"), "_"),  // "push_up" (wielokrotne spacje na _)
        ).distinct()
    }

    private fun checkFileExists(context: Context, path: String): Boolean {
        return try {
            val inputStream = context.assets.open(path)
            inputStream.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun findExerciseImagesInAssets(context: Context, exerciseName: String): List<String> {
        Log.d(TAG, "🔍 === WYSZUKIWANIE ZDJĘĆ DLA: '$exerciseName' ===")

        val foundImages = mutableListOf<String>()
        val nameVariants = generateNameVariants(exerciseName)

        nameVariants.forEach { variant ->
            val folderPath = "exercise_image/$variant"

            // Sprawdź różne rozszerzenia i indeksy
            for (index in 0..5) {
                listOf("jpg", "jpeg", "png", "webp").forEach { extension ->
                    val filePath = "$folderPath/$index.$extension"

                    if (checkFileExists(context, filePath)) {
                        foundImages.add("file:///android_asset/$filePath")
                        Log.d(TAG, "✅ Znaleziono: $filePath")
                    }
                }
            }
        }

        Log.d(TAG, "📋 Znalezione zdjęcia (${foundImages.size}):")
        foundImages.forEach { Log.d(TAG, "   - $it") }

        return foundImages
    }

    // Funkcja do sprawdzania konkretnej ścieżki
    fun testSpecificPath(context: Context, path: String) {
        Log.d(TAG, "🧪 === TEST KONKRETNEJ ŚCIEŻKI: '$path' ===")

        val exists = checkFileExists(context, path)
        Log.d(TAG, "${if (exists) "✅" else "❌"} Plik ${if (exists) "istnieje" else "nie istnieje"}: $path")

        if (exists) {
            try {
                val inputStream = context.assets.open(path)
                val size = inputStream.available()
                inputStream.close()
                Log.d(TAG, "📊 Rozmiar pliku: $size bajtów")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Błąd odczytu rozmiaru: ${e.message}")
            }
        }
    }
}