package mx.edu.utez.memorias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import mx.edu.utez.memorias.navigation.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as MemoriasApplication).appContainer
        setContent {
            MaterialTheme {
                AppNavGraph(appContainer = appContainer)
            }
        }
    }
}