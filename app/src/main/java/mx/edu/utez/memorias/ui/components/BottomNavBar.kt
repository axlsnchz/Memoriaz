package mx.edu.utez.memorias.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class HomeTab(val label: String) {
    EXPLORAR("Explorar"),
    MI_ALBUM("Mi Álbum")
}
@Composable
fun BottomNavBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == HomeTab.EXPLORAR,
            onClick = { onTabSelected(HomeTab.EXPLORAR) },
            icon = { Icon(
                Icons.Default.Explore, contentDescription =
                "Explorar") },
            label = { Text(HomeTab.EXPLORAR.label) }
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.MI_ALBUM,
            onClick = { onTabSelected(HomeTab.MI_ALBUM) },
            icon = { Icon(Icons.Default.PhotoAlbum, contentDescription = "Mi Álbum") },
            label = { Text(HomeTab.MI_ALBUM.label) }
        )
    }
}