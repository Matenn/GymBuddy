package com.kaczmarzykmarcin.GymBuddy.core.data.local.converter

import androidx.room.TypeConverter
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * Konwerter dla Room umożliwiający przechowywanie Timestamp w bazie danych
 */
class TimestampConverter {
    @TypeConverter
    fun fromTimestamp(timestamp: Timestamp?): Long? {
        return timestamp?.seconds
    }

    @TypeConverter
    fun toTimestamp(seconds: Long?): Timestamp? {
        return seconds?.let { Timestamp(it, 0) }
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(milliseconds: Long?): Date? {
        return milliseconds?.let { Date(it) }
    }
}

/**
 * Konwerter dla Room umożliwiający przechowywanie Map w bazie danych
 */
class MapConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromMapStringToString(map: Map<String, String>?): String? {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toMapStringFromString(value: String?): Map<String, String>? {
        if (value == null) return null
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun fromMapStringToAny(map: Map<String, Any>?): String? {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toMapStringFromAny(value: String?): Map<String, Any>? {
        if (value == null) return null
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun fromListMap(list: List<Map<String, Any>>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toListMap(value: String?): List<Map<String, Any>>? {
        if (value == null) return null
        val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
        return gson.fromJson(value, listType)
    }
}