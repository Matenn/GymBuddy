package com.kaczmarzykmarcin.GymBuddy

import android.app.Application
import android.util.Log
import com.kaczmarzykmarcin.GymBuddy.features.user.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Klasa aplikacji odpowiedzialna za inicjalizację komponentów aplikacji,
 * takich jak menedżer synchronizacji danych.
 */
@HiltAndroidApp
class GymBuddyApplication : Application() {

    @Inject
    lateinit var syncManager: SyncManager

    private val TAG = "GymBuddyApplication"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application created, initializing components")

        initializeSyncManager()
    }

    /**
     * Inicjalizuje menedżer synchronizacji danych
     */
    private fun initializeSyncManager() {
        Log.d(TAG, "Initializing SyncManager")
        syncManager.initialize()
    }
}