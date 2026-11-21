package mx.edu.utez.memorias.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import mx.edu.utez.memorias.data.remote.RetrofitClient
import mx.edu.utez.memorias.ui.components.MapLibreLocationView
import mx.edu.utez.memorias.ui.viewmodels.AddEditMemoryEvent
import mx.edu.utez.memorias.ui.viewmodels.AddEditMemoryViewModel

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemoryScreen(
    memoryId: Int?,
    viewModel: AddEditMemoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingPhotoFile != null) {
            viewModel.onPhotoCaptured(pendingPhotoFile!!.absolutePath)
        }
    }
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.detectCurrentLocation(context)
        }
    }
    LaunchedEffect(memoryId) {
        viewModel.loadMemoryForEdit(memoryId)
        if (memoryId == null || memoryId <= 0) {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AddEditMemoryEvent.SavedSuccess) {
                onNavigateBack()
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Editar Recuerdo" else
                    "Nuevo Recuerdo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Vista previa fotográfica
            val imageModel: Any? = when {
                !state.localImagePath.isNullOrBlank() ->
                    File(state.localImagePath!!)
                !state.remoteImageFilename.isNullOrBlank() ->
                    RetrofitClient.getImageUrl(state.remoteImageFilename)
                else -> null
            }
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Fotografía del recuerdo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            if (!state.isEditMode) {
                OutlinedButton(
                    onClick = {
                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        val storageDir =
                            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        val file = File.createTempFile("MEM_${timeStamp}_", ".jpg", storageDir)
                        pendingPhotoFile = file
                        val uri: Uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        takePictureLauncher.launch(uri)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (imageModel == null) "Tomar Fotografía" else
                        "Volver a Tomar Foto")
                }
            }
            OutlinedTextField(
                value = state.titulo,
                onValueChange = viewModel::onTituloChange,
                label = { Text("Título del recuerdo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.lugar,
                onValueChange = viewModel::onLugarChange,
                label = { Text("Lugar (GPS / Geocoder)") },
                trailingIcon = {
                    IconButton(onClick = {
                        viewModel.detectCurrentLocation(context) }) {
                        Icon(Icons.Default.MyLocation, contentDescription ="Detectar GPS")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Toca el mapa para ajustar la ubicación (${"%.4f".format(state.latitud)}, ${"%.4f".format(state.longitud)})",
            style = MaterialTheme.typography.labelMedium
            )
            MapLibreLocationView(
                latitud = state.latitud,
                longitud = state.longitud,
                markerTitle = state.lugar.ifBlank { "Ubicación del recuerdo"},
                interactive = true,
                onLocationSelected = { lat, lng ->
                    viewModel.onMapCoordinatesSelected(lat, lng, context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            OutlinedTextField(
                value = state.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                label = { Text("Descripción / Crónica del viaje") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Marca de tiempo: ${state.captureTimestamp}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style =MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = viewModel::saveMemory,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier =
                        Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (state.isEditMode) "Guardar Cambios" else
                        "Guardar Recuerdo")
                }
            }
        }
    }
}

