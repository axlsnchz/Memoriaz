package mx.edu.utez.memorias.navigation

sealed class AppRoutes(val route: String) {
    data object Login : AppRoutes("login")
    data object Register : AppRoutes("register")
    data object Home : AppRoutes("home")
    data object AddEditMemory :
        AppRoutes("add_edit_memory?memoryId={memoryId}") {
        fun createRoute(memoryId: Int? = null): String =
            if (memoryId != null) "add_edit_memory?memoryId=$memoryId" else"add_edit_memory"
    }
}