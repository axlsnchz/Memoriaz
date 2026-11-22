package mx.edu.utez.memorias

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import mx.edu.utez.memorias.navigation.AppNavGraph
import mx.edu.utez.memorias.workers.LikeNotificationWorker

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult( ActivityResultContracts.RequestPermission()
    ) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val appContainer = (application as MemoriasApplication).appContainer

        setContent {
            MaterialTheme {
                AppNavGraph(appContainer = appContainer)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        WorkManager.getInstance(applicationContext).enqueue(
            OneTimeWorkRequestBuilder<LikeNotificationWorker>().build() )
    }
}

