package mx.edu.utez.memorias.ui.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import mx.edu.utez.memorias.data.local.UserPreferencesRepository
import mx.edu.utez.memorias.data.repository.MemoryRepository
import java.time.Instant

import java.util.Locale
import kotlin.coroutines.resume

data class AddEditMemoryUiState(
    val isEditMode: Boolean = false,
    val memoryId: Int? = null,
    val titulo: String = "",
    val lugar: String = "",
    val latitud: Double = 18.8825, // Coordenada inicial por defecto
    val longitud: Double = -99.1806,
    val descripcion: String = "",
    val localImagePath: String? = null,
    val remoteImageFilename: String? = null,
    val captureTimestamp: String = Instant.now().toString(),
    val isLoadingLocation: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
sealed interface AddEditMemoryEvent {
    data class SavedSuccess(val message: String) : AddEditMemoryEvent
}
class AddEditMemoryViewModel(
    private val memoryRepository: MemoryRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddEditMemoryUiState())
    val uiState: StateFlow<AddEditMemoryUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<AddEditMemoryEvent>()
    val events: SharedFlow<AddEditMemoryEvent> = _events.asSharedFlow()
    fun loadMemoryForEdit(memoryId: Int?) {
        if (memoryId == null || memoryId <= 0) return
        viewModelScope.launch {
            val existing = memoryRepository.getMemoryById(memoryId) ?:
            return@launch
            _uiState.update {
                it.copy(
                    isEditMode = true,
                    memoryId = existing.id,
                    titulo = existing.titulo,
                    lugar = existing.lugar,
                    latitud = existing.latitud,
                    longitud = existing.longitud,
                    descripcion = existing.descripcion,
                    localImagePath = existing.localImagePath,
                    remoteImageFilename = existing.imageFilename,
                    captureTimestamp = existing.timestamp
                )
            }
        }
    }
    fun onTituloChange(value: String) = _uiState.update { it.copy(titulo =value, errorMessage = null) }
    fun onLugarChange(value: String) = _uiState.update { it.copy(lugar =
        value, errorMessage = null) }
    fun onDescripcionChange(value: String) = _uiState.update {
        it.copy(descripcion = value, errorMessage = null) }
    fun onPhotoCaptured(filePath: String) {
        _uiState.update {
            it.copy(
                localImagePath = filePath,
                captureTimestamp = Instant.now().toString()
            )
        }
    }
    fun onMapCoordinatesSelected(lat: Double, lng: Double, context: Context) {
        _uiState.update { it.copy(latitud = lat, longitud = lng) }
        resolvePlaceName(context, lat, lng)
    }
    @SuppressLint("MissingPermission")
    fun detectCurrentLocation(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true) }
            try {
                val fusedClient =
                    LocationServices.getFusedLocationProviderClient(context)
                val location =
                    fusedClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null).await()
                if (location != null) {
                    _uiState.update {
                        it.copy(latitud = location.latitude, longitud =
                            location.longitude)
                    }
                    resolvePlaceName(context, location.latitude,
                        location.longitude)
                }
            } catch (_: Exception) {
                // Mantiene coordenadas actuales si GPS no responde
            } finally {
                _uiState.update { it.copy(isLoadingLocation = false) }
            }
        }
    }
    private fun resolvePlaceName(context: Context, lat: Double, lng: Double) {
        viewModelScope.launch {
            val placeName = reverseGeocode(context, lat, lng)
            if (!placeName.isNullOrBlank()) {
                _uiState.update { it.copy(lugar = placeName) }
            }
        }
    }
    private suspend fun reverseGeocode(context: Context, lat: Double, lng:Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(lat, lng, 1) { addresses ->
                val addr = addresses.firstOrNull()
                val name = listOfNotNull(addr?.locality,
                    addr?.adminArea, addr?.countryName)
                    .distinct()
                    .joinToString(", ")
                cont.resume(name.ifBlank { null })
            }
        }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                val addr = geocoder.getFromLocation(lat, lng,
                    1)?.firstOrNull()
                listOfNotNull(addr?.locality, addr?.adminArea,
                    addr?.countryName)
                    .distinct()
                    .joinToString(", ")
                    .ifBlank { null }
            }
        }
    }
    fun saveMemory() {
        val state = _uiState.value
        if (state.titulo.isBlank() || state.lugar.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El título y el lugar son obligatorios") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val session = preferencesRepository.userSessionFlow.first()
            if (state.isEditMode && state.memoryId != null) {
                memoryRepository.updateMemory(
                    userMatricula = session.matricula,
                    memoryId = state.memoryId,
                    titulo = state.titulo.trim(),
                    lugar = state.lugar.trim(),
                    latitud = state.latitud,
                    longitud = state.longitud,
                    descripcion = state.descripcion.trim()


                    )
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(AddEditMemoryEvent.SavedSuccess("Recuerdo actualizado y registrado en historial"))
            } else {
                val result = memoryRepository.createMemory(
                    userMatricula = session.matricula,
                    titulo = state.titulo.trim(),
                    lugar = state.lugar.trim(),
                    latitud = state.latitud,
                    longitud = state.longitud,
                    descripcion = state.descripcion.trim(),
                    localImagePath = state.localImagePath,
                    captureTimestamp = state.captureTimestamp
                )
                _uiState.update { it.copy(isSaving = false) }
                val synced = result.getOrDefault(false)
                val msg = if (synced) {
                    "Recuerdo publicado en la nube"
                } else {
                    "Guardado sin conexión: se sincronizará automáticamente" }
                _events.emit(AddEditMemoryEvent.SavedSuccess(msg))
            }
        }
    }
}