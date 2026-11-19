package mx.edu.utez.memorias

import android.content.Context
import mx.edu.utez.memorias.data.local.AppDatabase
import mx.edu.utez.memorias.data.local.UserPreferencesRepository
import mx.edu.utez.memorias.data.remote.ApiService
import mx.edu.utez.memorias.data.remote.RetrofitClient
import mx.edu.utez.memorias.data.repository.AuthRepository

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    val userPreferencesRepository = UserPreferencesRepository(context)
    val apiService: ApiService = RetrofitClient.apiService

    val authRepository = AuthRepository(
        apiService = apiService,
        userDao = database.userDao(),
        preferencesRepository = userPreferencesRepository
    )
}