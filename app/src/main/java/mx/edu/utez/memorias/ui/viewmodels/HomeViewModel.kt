package mx.edu.utez.memorias.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.edu.utez.memorias.data.local.UserPreferencesRepository
import mx.edu.utez.memorias.data.local.entity.MemoryEntity
import mx.edu.utez.memorias.data.repository.AuthRepository
import mx.edu.utez.memorias.data.repository.MemoryRepository
import mx.edu.utez.memorias.ui.components.HomeTab

data class HomeUiState(
    val selectedTab: HomeTab = HomeTab.EXPLORAR,
    val memories: List<MemoryEntity> = emptyList(),
    val currentUserName: String = "",
    val currentMatricula: String = "",
    val isRefreshing: Boolean = false,
    val shakeFeedback: String? = null
)
sealed interface HomeEvent {
    data object NavigateToLogin : HomeEvent
    data class ShowMessage(val message: String) : HomeEvent
}
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val memoryRepository: MemoryRepository,
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository

) : ViewModel() {
    private val _selectedTab = MutableStateFlow(HomeTab.EXPLORAR)
    private val _isRefreshing = MutableStateFlow(false)
    private val _shakeFeedback = MutableStateFlow<String?>(null)
    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()
    val uiState: StateFlow<HomeUiState> = combine(
        _selectedTab,
        preferencesRepository.userSessionFlow,
        _isRefreshing,
        _shakeFeedback
    ) { tab, session, refreshing, shakeMsg ->
        Quad(tab, session, refreshing, shakeMsg)
    }.flatMapLatest { (tab, session, refreshing, shakeMsg) ->
        val memoriesFlow = if (tab == HomeTab.EXPLORAR) {
            memoryRepository.observeAllMemories()
        } else {
            memoryRepository.observeUserMemories(session.matricula)
        }
        memoriesFlow.map { list ->
            HomeUiState(
                selectedTab = tab,
                memories = list,
                currentUserName = "${session.nombres} ${session.apellidos}".trim(),
            currentMatricula = session.matricula,
            isRefreshing = refreshing,
            shakeFeedback = shakeMsg
            )
        }
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        HomeUiState())
    init {
        refreshMemories()
    }
    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }
    fun refreshMemories() {
        viewModelScope.launch {
            _isRefreshing.value = true
            memoryRepository.syncPendingOfflineMemories()
            memoryRepository.refreshMemoriesFromRemote()
                .onFailure {
                    _events.emit(HomeEvent.ShowMessage("Modo sin conexión: mostrando caché local"))
                }
            _isRefreshing.value = false
        }
    }
    fun onShakeDetected() {
        viewModelScope.launch {
            _shakeFeedback.value = "Agitación detectada: actualizando álbum..."
            refreshMemories()
            kotlinx.coroutines.delay(2000)
            _shakeFeedback.value = null
        }
    }
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _events.emit(HomeEvent.NavigateToLogin)
        }
    }
    private data class Quad<A, B, C, D>(val first: A, val second: B, val
    third: C, val fourth: D)
}