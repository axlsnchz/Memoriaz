package mx.edu.utez.memorias.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import mx.edu.utez.memorias.MemoriasApplication

class LikeNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as
                MemoriasApplication).appContainer
        val session =
            container.userPreferencesRepository.userSessionFlow.first()
        if (!session.isLoggedIn || session.matricula.isBlank()) {
            return Result.success()
        }
        return try {
            val response =
                container.apiService.getPendingNotifications(session.matricula)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) { createNotificationChannel()
                response.body()!!.forEach { notif ->
                    showNativeNotification(
                        id = notif.id,
                        title = "¡Nuevo Like en ${notif.memoryTitulo}!",
                        body = "${notif.likerName} (${notif.likerMatricula}) reaccionó a tu recuerdo."
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reacciones y Likes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones cuando alguien da like a tus recuerdos"
            }
            val manager =
                applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    private fun showNativeNotification(id: Int, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id,
            notification)
    }
    companion object {
        private const val CHANNEL_ID = "memorias_likes_channel"
    }
}