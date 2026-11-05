package fr.matiasceau.tasks.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(val baseUrl: String, val apiKey: String)

const val DEFAULT_BASE_URL = "https://api.matias-ceau.fr"

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val baseUrlKey = stringPreferencesKey("base_url")
    private val apiKeyKey = stringPreferencesKey("api_key")

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            baseUrl = prefs[baseUrlKey] ?: DEFAULT_BASE_URL,
            apiKey = prefs[apiKeyKey].orEmpty(),
        )
    }

    suspend fun save(settings: Settings) {
        context.dataStore.edit { prefs ->
            prefs[baseUrlKey] = settings.baseUrl.trim()
            prefs[apiKeyKey] = settings.apiKey.trim()
        }
    }
}
