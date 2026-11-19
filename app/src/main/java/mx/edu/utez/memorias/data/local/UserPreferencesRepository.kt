package mx.edu.utez.memorias.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
private val Context.dataStore: DataStore<Preferences> by
preferencesDataStore(name = "user_session")
data class UserSession(
    val matricula: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val isLoggedIn: Boolean = false
)
class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val MATRICULA = stringPreferencesKey("matricula")
        val NOMBRES = stringPreferencesKey("nombres")
        val APELLIDOS = stringPreferencesKey("apellidos")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }
    val userSessionFlow: Flow<UserSession> = context.dataStore.data.map { prefs ->
        UserSession(
            matricula = prefs[Keys.MATRICULA] ?: "",
            nombres = prefs[Keys.NOMBRES] ?: "",
            apellidos = prefs[Keys.APELLIDOS] ?: "",
            isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false
        )
    }
    suspend fun saveSession(matricula: String, nombres: String, apellidos: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MATRICULA] = matricula
            prefs[Keys.NOMBRES] = nombres
            prefs[Keys.APELLIDOS] = apellidos
            prefs[Keys.IS_LOGGED_IN] = true
        }
    }
    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
