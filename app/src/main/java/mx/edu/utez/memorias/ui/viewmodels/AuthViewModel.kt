package mx.edu.utez.memorias.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mx.edu.utez.memorias.data.local.UserSession
import mx.edu.utez.memorias.data.repository.AuthRepository

data class AuthUiState(
    val matricula: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
sealed interface AuthEvent {
    data object NavigateToHome : AuthEvent
}
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()
    val sessionState: StateFlow<UserSession?> = authRepository.sessionFlow .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onMatriculaChange(value: String) {
        _uiState.update { it.copy(matricula = value, errorMessage = null) } }
    fun onNombresChange(value: String) {
        _uiState.update { it.copy(nombres = value, errorMessage = null) } }
    fun onApellidosChange(value: String) {
        _uiState.update { it.copy(apellidos = value, errorMessage = null) } }
    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) } }
    fun login() {
        val state = _uiState.value
        if (state.matricula.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa matrícula y contraseña") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.login(state.matricula, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, password = "") }
                    _events.emit(AuthEvent.NavigateToHome)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false,
                        errorMessage = error.message) }
                }
        }
    }
    fun register() {
        val state = _uiState.value
        if (state.matricula.isBlank() || state.nombres.isBlank() ||
            state.apellidos.isBlank() || state.password.length < 4
        ) {
            _uiState.update {
                it.copy(errorMessage = "Completa todos los campos(contraseña mín. 4 caracteres)")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.register(state.matricula, state.nombres,
                state.apellidos, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, password = "") }
                    _events.emit(AuthEvent.NavigateToHome)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false,
                        errorMessage = error.message) }
                }
        }
    }
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}