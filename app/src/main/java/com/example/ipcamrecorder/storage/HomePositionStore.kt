package com.example.ipcamrecorder.storage

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "home_prefs")

data class HomePosition(val pan: Double, val tilt: Double, val zoom: Double)

class HomePositionStore(private val context: Context) {
    companion object {
        val PAN_KEY = doublePreferencesKey("home_pan")
        val TILT_KEY = doublePreferencesKey("home_tilt")
        val ZOOM_KEY = doublePreferencesKey("home_zoom")
    }

    val homePositionFlow = context.dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }.map { prefs ->
        val p = prefs[PAN_KEY] ?: 0.0
        val t = prefs[TILT_KEY] ?: 0.0
        val z = prefs[ZOOM_KEY] ?: 0.0
        HomePosition(p, t, z)
    }

    suspend fun saveHome(position: HomePosition) {
        context.dataStore.edit { prefs ->
            prefs[PAN_KEY] = position.pan
            prefs[TILT_KEY] = position.tilt
            prefs[ZOOM_KEY] = position.zoom
        }
    }
}
