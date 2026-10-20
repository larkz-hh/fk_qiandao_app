package io.github.yesterdaylz.kxqiandao.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "data")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_UID = stringPreferencesKey("uid")
        private val KEY_UNAME = stringPreferencesKey("uname")
        private val START_TIME_KEY = stringPreferencesKey("start_time")

        private val KEY_WEEK = stringPreferencesKey("week")
        private val KEY_TOTAL_TIME = stringPreferencesKey("totalTime")
        private val KEY_HEATMAP = stringPreferencesKey("heatmap_cache")
        private val KEY_RECORDS = stringPreferencesKey("records_cache")
    }

    val uid: Flow<String> = context.dataStore.data.map { it[KEY_UID] ?: "null" }
    val uname: Flow<String> = context.dataStore.data.map { it[KEY_UNAME] ?: "null" }
    val startTime: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[START_TIME_KEY] ?: "null"
    }
    val week: Flow<String> = context.dataStore.data.map { it[KEY_WEEK] ?: "null" }
    val totalTime: Flow<String> = context.dataStore.data.map { it[KEY_TOTAL_TIME] ?: "null" }
    val heatmapCache: Flow<String> = context.dataStore.data.map { it[KEY_HEATMAP] ?: "" }
    val recordsCache: Flow<String> = context.dataStore.data.map { it[KEY_RECORDS] ?: "" }

    suspend fun saveUid(value: String) {
        context.dataStore.edit { it[KEY_UID] = value }
    }

    suspend fun saveUname(value: String) {
        context.dataStore.edit { it[KEY_UNAME] = value }
    }


    suspend fun saveWeek(value: String) {
        context.dataStore.edit { it[KEY_WEEK] = value }
    }
    suspend fun saveStartTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[START_TIME_KEY] = time
        }
    }
    suspend fun saveTotalTime(value: String) {
        context.dataStore.edit { it[KEY_TOTAL_TIME] = value }
    }

    suspend fun saveHeatmapCache(value: String) {
        context.dataStore.edit { it[KEY_HEATMAP] = value }
    }

    suspend fun saveRecordsCache(value: String) {
        context.dataStore.edit { it[KEY_RECORDS] = value }
    }
}