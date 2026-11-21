package mx.edu.utez.memorias.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.edu.utez.memorias.ui.components.BottomNavBar
import mx.edu.utez.memorias.ui.components.HomeTab
import mx.edu.utez.memorias.ui.components.MemoryCard
import mx.edu.utez.memorias.ui.components.ShakeDetector
import mx.edu.utez.memorias.ui.viewmodels.HomeEvent
import mx.edu.utez.memorias.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddMemory: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    ShakeDetector(onShake = viewModel::onShakeDetected)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.NavigateToLogin -> onLoggedOut()
                is HomeEvent.ShowMessage ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Memorias")
                        if (state.currentUserName.isNotBlank()) {
                            Text(
                                text = state.currentUserName,
                                style =
                                    MaterialTheme.typography.labelMedium,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::logout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = state.selectedTab,
                onTabSelected = viewModel::selectTab
            )
        },
        floatingActionButton = {
            if (state.selectedTab == HomeTab.MI_ALBUM) {
                FloatingActionButton(onClick = onNavigateToAddMemory) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo recuerdo")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            state.shakeFeedback?.let { feedback ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feedback,
                        modifier = Modifier.padding(horizontal = 16.dp,
                            vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refreshMemories,
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.memories.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) { Text(
                        text = if (state.selectedTab ==
                            HomeTab.EXPLORAR)
                            "Aún no hay recuerdos publicados. ¡Agita para sincronizar!"
                        else
                            "Tu álbum está vacío. Presiona + para capturar tu primer viaje.",
                                    style = MaterialTheme.typography.bodyLarge
                    )
                    }
                } else {
                    // Distribución estricta en 2 columnas usando LazyColumn + .chunked(2)
                    val rows = remember(state.memories) {
                        state.memories.chunked(2) }
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()
                    ) {
                        items(rows, key = { row -> row.joinToString("-") { it.id.toString() } }) { rowItems ->
                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowItems.forEach { memory ->
                                    MemoryCard(
                                        memory = memory,
                                        onClick = {
                                            onNavigateToDetail(memory.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}