package mx.edu.utez.memorias.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mx.edu.utez.memorias.MemoriasApplication

class SyncMemoriesWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as
                MemoriasApplication).appContainer
        return try {
            container.memoryRepository.syncPendingOfflineMemories()
            container.memoryRepository.refreshMemoriesFromRemote()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}