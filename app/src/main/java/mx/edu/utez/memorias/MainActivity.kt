package mx.edu.utez.memorias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mx.edu.utez.memorias.data.local.UserSession
import mx.edu.utez.memorias.navigation.AppRoutes
import mx.edu.utez.memorias.ui.screens.LoginScreen
import mx.edu.utez.memorias.ui.screens.RegisterScreen
import mx.edu.utez.memorias.ui.theme.MemoriasTheme
import mx.edu.utez.memorias.ui.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as MemoriasApplication).appContainer

        setContent {
            MaterialTheme {
                Stage1App(appContainer = appContainer)
            }
        }
    }
}

@Composable
private fun Stage1App(appContainer: AppContainer) {
    val navController = rememberNavController()

    val authViewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(appContainer.authRepository) as T
            }
        }
    )

    val session by authViewModel.sessionState.collectAsStateWithLifecycle()

    // Esperar a que DataStore emita el estado inicial de la sesión
    val currentSession = session ?: return
    val startDestination = if (currentSession.isLoggedIn) {
        AppRoutes.Home.route
    } else {
        AppRoutes.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppRoutes.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(AppRoutes.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(AppRoutes.Home.route) {
                        popUpTo(AppRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(AppRoutes.Home.route) {
                        popUpTo(AppRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.Home.route) {
            Stage1HomePlaceholder(
                session = currentSession,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(AppRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
private fun Stage1HomePlaceholder(
    session: UserSession,
    onLogout: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sesión Activa (Etapa 1)",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${session.nombres} ${session.apellidos}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Matrícula: ${session.matricula}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onLogout) {
                Text("Cerrar Sesión")
            }
        }
    }
}