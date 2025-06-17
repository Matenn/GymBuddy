package com.kaczmarzykmarcin.GymBuddy.core.utils

import android.content.Context
import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import java.io.IOException

class AssetImageFetcher(
    private val data: String,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val assetPath = data.removePrefix("asset://")

        return try {
            val inputStream = context.assets.open(assetPath)
            val drawable = Drawable.createFromStream(inputStream, null)
            inputStream.close()

            if (drawable != null) {
                DrawableResult(
                    drawable = drawable,
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            } else {
                throw IOException("Failed to decode asset: $assetPath")
            }
        } catch (e: IOException) {
            throw e
        }
    }

    class Factory : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (data.startsWith("asset://")) {
                AssetImageFetcher(data, options.context)
            } else {
                null
            }
        }
    }
}

// Funkcja pomocnicza do budowania ImageLoader z obsługą assets
fun createImageLoaderWithAssets(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(AssetImageFetcher.Factory())
        }
        .build()
}

// Funkcja do generowania URI dla zdjęć z assets
fun getExerciseImageUri(exerciseName: String, imageIndex: Int): String {
    val formattedName = exerciseName.lowercase().replace(" ", "_")
    return "asset://exercise_image/${formattedName}/${imageIndex}.jpg"
}

// Funkcja do sprawdzania czy plik istnieje w assets
fun assetFileExists(context: Context, path: String): Boolean {
    return try {
        context.assets.open(path).close()
        true
    } catch (e: Exception) {
        false
    }
}

// Funkcja do pobierania dostępnych zdjęć ćwiczenia
fun getAvailableExerciseImages(context: Context, exerciseName: String): List<String> {
    val images = mutableListOf<String>()
    val formattedName = exerciseName.lowercase().replace(" ", "_")

    for (i in 0..1) {
        val assetPath = "exercise_image/${formattedName}/${i}.jpg"
        if (assetFileExists(context, assetPath)) {
            images.add(getExerciseImageUri(exerciseName, i))
        }
    }

    return images
}