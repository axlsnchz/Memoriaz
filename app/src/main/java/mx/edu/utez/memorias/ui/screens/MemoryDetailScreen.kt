package mx.edu.utez.memorias.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import mx.edu.utez.memorias.data.remote.RetrofitClient
import mx.edu.utez.memorias.ui.components.EditHistoryDialog
import mx.edu.utez.memorias.ui.components.MapLibreLocationView
import mx.edu.utez.memorias.ui.viewmodels.MemoryDetailEvent
import mx.edu.utez.memorias.ui.viewmodels.MemoryDetailViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    memoryId: Int,
    viewModel: MemoryDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val memory = state.memory
    val isDeleted = memory?.isDeleted == 1
    LaunchedEffect(memoryId) {
        viewModel.loadMemory(memoryId)
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is MemoryDetailEvent.DeletedAndNavigateBack) {
                onNavigateBack()
            }
        }
    }
    if (state.showHistoryDialog) {
        EditHistoryDialog(
            history = state.editHistory,
            onDismiss = { viewModel.setHistoryDialogVisible(false) }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(memory?.titulo ?: "Detalle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.setHistoryDialogVisible(true) }) {
                        Icon(Icons.Default.History, contentDescription =
                            "Historial de cambios")
                    }
                    // Solo el propietario puede editar o eliminar lógicamente
                    if (state.isOwner && !isDeleted && memory != null) {
                        IconButton(onClick = { onNavigateToEdit(memory.id) }) {
                            Icon(Icons.Default.Edit, contentDescription ="Editar")
                        }
                        IconButton(onClick = viewModel::logicalDelete) {
                            Icon(Icons.Default.Delete, contentDescription ="Eliminar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (memory == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isDeleted) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Recuerdo eliminado (solo lectura)",
                        color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp)
                    )
                }
            }
            val imageModel: Any? = when {
                !memory.localImagePath.isNullOrBlank() &&
                        File(memory.localImagePath).exists() ->
                    File(memory.localImagePath)
                !memory.imageFilename.isNullOrBlank() ->
                    RetrofitClient.getImageUrl(memory.imageFilename)
                else -> null
            }
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = memory.titulo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = memory.titulo,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${memory.lugar} • Autor:${memory.userMatricula}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant )
                }
                FilledTonalButton(
                    onClick = viewModel::toggleLike,
                    enabled = !isDeleted
                ) {
                    Icon(
                        imageVector = if (state.isLikedByMe)
                            Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Me gusta",
                        tint = if (state.isLikedByMe)
                            MaterialTheme.colorScheme.error else LocalContentColor.current
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${state.likesCount} likes")
                }
            }
            Text(
                text = memory.descripcion,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Ubicación geográfica",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            MapLibreLocationView(
                latitud = memory.latitud,
                longitud = memory.longitud,
                markerTitle = memory.lugar,
                interactive = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            // Resumen cronológico en línea del historial
            if (state.editHistory.isNotEmpty()) {
                Text(
                    text = "Historial de ediciones(${state.editHistory.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
                )
                state.editHistory.forEach { entry ->
                    Text(
                        text = "• [${entry.editedAt}]${entry.changeSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant )
                }
            }
        }
    }
}