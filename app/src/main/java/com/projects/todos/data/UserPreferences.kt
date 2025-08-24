package com.projects.todos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    
    companion object {
        private val USER_NAME = stringPreferencesKey("user_name")
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val NOTIFICATIONS_PERMISSION_GRANTED = booleanPreferencesKey("notifications_permission_granted")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val DEFAULT_REMINDER_OFFSET_MINUTES = intPreferencesKey("default_reminder_offset_minutes")
    }
    
    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME]
    }
    
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }
    
    val isNotificationsPermissionGranted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_PERMISSION_GRANTED] ?: false
    }
    
    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED] ?: true
    }
    
    val defaultReminderOffsetMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_REMINDER_OFFSET_MINUTES] ?: 5
    }
    
    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }
    
    suspend fun setNotificationsPermissionGranted(granted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_PERMISSION_GRANTED] = granted
        }
    }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    suspend fun setDefaultReminderOffsetMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_REMINDER_OFFSET_MINUTES] = minutes
        }
    }
}
