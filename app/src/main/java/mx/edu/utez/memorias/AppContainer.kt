package mx.edu.utez.memorias

import android.content.Context
import mx.edu.utez.memorias.data.local.AppDatabase
import mx.edu.utez.memorias.data.local.UserPreferencesRepository
import mx.edu.utez.memorias.data.remote.ApiService
import mx.edu.utez.memorias.data.remote.RetrofitClient
import mx.edu.utez.memorias.data.repository.AuthRepository
import mx.edu.utez.memorias.data.repository.MemoryRepository

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    val userPreferencesRepository = UserPreferencesRepository(context)
    val apiService: ApiService = RetrofitClient.apiService
    val authRepository = AuthRepository(
        apiService = apiService,
        userDao = database.userDao(),
        preferencesRepository = userPreferencesRepository
    )
    val memoryRepository = MemoryRepository(
        apiService = apiService,
        memoryDao = database.memoryDao(),
        userDao = database.userDao(),
        likeDao = database.likeDao(),
        editHistoryDao = database.editHistoryDao()
    )
}