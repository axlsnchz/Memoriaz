package mx.edu.utez.memorias.navigation

sealed class AppRoutes(val route: String) {
    data object Login : AppRoutes("login")
    data object Register : AppRoutes("register")
    data object Home : AppRoutes("home")
}