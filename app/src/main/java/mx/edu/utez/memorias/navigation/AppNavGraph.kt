package mx.edu.utez.memorias.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.edu.utez.memorias.AppContainer
import mx.edu.utez.memorias.ui.screens.AddEditMemoryScreen
import mx.edu.utez.memorias.ui.screens.HomeScreen
import mx.edu.utez.memorias.ui.screens.LoginScreen
import mx.edu.utez.memorias.ui.screens.MemoryDetailScreen
import mx.edu.utez.memorias.ui.screens.RegisterScreen
import mx.edu.utez.memorias.ui.viewmodels.AddEditMemoryViewModel
import mx.edu.utez.memorias.ui.viewmodels.AuthViewModel
import mx.edu.utez.memorias.ui.viewmodels.HomeViewModel
import mx.edu.utez.memorias.ui.viewmodels.MemoryDetailViewModel


@Composable
fun AppNavGraph(appContainer: AppContainer) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = simpleFactory { AuthViewModel(appContainer.authRepository)
    })
    val session by authViewModel.sessionState.collectAsStateWithLifecycle() // Esperar carga inicial de DataStore antes de montar el NavHost
    val currentSession = session ?: return
    val startDestination = if (currentSession.isLoggedIn)
        AppRoutes.Home.route else AppRoutes.Login.route
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppRoutes.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(AppRoutes.Register.route) },
                onLoginSuccess = {
                    navController.navigate(AppRoutes.Home.route) {
                        popUpTo(AppRoutes.Login.route) { inclusive = true } }
                }
            )
        }
        composable(AppRoutes.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(AppRoutes.Home.route) {
                        popUpTo(AppRoutes.Login.route) { inclusive = true } }
                }
            )
        }
        composable(AppRoutes.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(factory =
                simpleFactory {
                    HomeViewModel(
                        appContainer.memoryRepository,
                        appContainer.authRepository,
                        appContainer.userPreferencesRepository
                    )
                })
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToAddMemory = {

                    navController.navigate(AppRoutes.AddEditMemory.createRoute())
                },
                onNavigateToDetail = { memoryId ->

                    navController.navigate(AppRoutes.MemoryDetail.createRoute(memoryId))
                },
                onLoggedOut = {
                    navController.navigate(AppRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = AppRoutes.AddEditMemory.route,
            arguments = listOf(
                navArgument("memoryId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getInt("memoryId") ?: -1
            val addEditViewModel: AddEditMemoryViewModel = viewModel(factory = simpleFactory {
            AddEditMemoryViewModel(
                appContainer.memoryRepository,
                appContainer.userPreferencesRepository
            )
        })
            AddEditMemoryScreen(
                memoryId = rawId.takeIf { it > 0 },
                viewModel = addEditViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = AppRoutes.MemoryDetail.route,
            arguments = listOf(navArgument("memoryId") { type =
                NavType.IntType })
        ) { backStackEntry ->
            val memoryId = backStackEntry.arguments?.getInt("memoryId") ?:return@composable
            val detailViewModel: MemoryDetailViewModel = viewModel(factory =simpleFactory {
                MemoryDetailViewModel(
                    appContainer.memoryRepository,
                    appContainer.userPreferencesRepository
                )
            })
            MemoryDetailScreen(
                memoryId = memoryId,
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->

                    navController.navigate(AppRoutes.AddEditMemory.createRoute(id))
                }
            )
        }
    }
}
private inline fun <reified T : ViewModel> simpleFactory(
    crossinline creator: () -> T
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM =
        creator() as VM
}