package mx.edu.utez.memorias

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import mx.edu.utez.memorias.workers.LikeNotificationWorker
import mx.edu.utez.memorias.workers.SyncMemoriesWorker
import java.util.concurrent.TimeUnit

class MemoriasApplication : Application() {
    lateinit var appContainer: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        scheduleBackgroundWorkers()
    }
    private fun scheduleBackgroundWorkers() {
        val workManager = WorkManager.getInstance(this)
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        // 1. Sincronización periódica y al recuperar conexión
        val syncRequest = PeriodicWorkRequestBuilder<SyncMemoriesWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "sync_offline_memories",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        // 2. Consulta periódica de likes nuevos incluso con la app cerrada

        val likeNotificationRequest =
        PeriodicWorkRequestBuilder<LikeNotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "poll_pending_like_notifications",
            ExistingPeriodicWorkPolicy.KEEP,
            likeNotificationRequest
        )
    }
}