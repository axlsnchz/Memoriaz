package mx.edu.utez.memorias.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.edu.utez.memorias.data.local.UserPreferencesRepository
import mx.edu.utez.memorias.data.local.entity.EditHistoryEntity
import mx.edu.utez.memorias.data.local.entity.MemoryEntity
import mx.edu.utez.memorias.data.repository.MemoryRepository

data class MemoryDetailUiState(
    val memory: MemoryEntity? = null,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val isOwner: Boolean = false,
    val editHistory: List<EditHistoryEntity> = emptyList(),
    val showHistoryDialog: Boolean = false
)
sealed interface MemoryDetailEvent {
    data object DeletedAndNavigateBack : MemoryDetailEvent
    data class ShowError(val message: String) : MemoryDetailEvent
}
@OptIn(ExperimentalCoroutinesApi::class)
class MemoryDetailViewModel(
    private val memoryRepository: MemoryRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _memoryId = MutableStateFlow<Int?>(null)
    private val _showHistoryDialog = MutableStateFlow(false)
    private val _events = MutableSharedFlow<MemoryDetailEvent>()
    val events: SharedFlow<MemoryDetailEvent> = _events.asSharedFlow()
    val uiState: StateFlow<MemoryDetailUiState> = combine(
        _memoryId.filterNotNull(),
        preferencesRepository.userSessionFlow,
        _showHistoryDialog
    ) { id, session, showDialog ->
        Triple(id, session.matricula, showDialog)
    }.flatMapLatest { (id, currentMatricula, showDialog) ->
        combine(
            memoryRepository.observeMemoryById(id),
            memoryRepository.observeLikeCount(id),
            memoryRepository.observeIsLikedByUser(id, currentMatricula),
            memoryRepository.observeEditHistory(id)
        ) { memory, likesCount, isLiked, history ->
            MemoryDetailUiState(
                memory = memory,
                likesCount = likesCount,
                isLikedByMe = isLiked,
                isOwner = memory?.userMatricula == currentMatricula,
                editHistory = history,
                showHistoryDialog = showDialog
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        MemoryDetailUiState())
    fun loadMemory(memoryId: Int) {
        _memoryId.value = memoryId
        viewModelScope.launch {
            memoryRepository.refreshEditHistory(memoryId)
        }
    }
    fun toggleLike() {
        val memory = uiState.value.memory ?: return
        if (memory.isDeleted == 1) return
        viewModelScope.launch {
            val session = preferencesRepository.userSessionFlow.first()
            memoryRepository.toggleLike(memory.id, session.matricula)
        }
    }
    fun logicalDelete() {
        val memory = uiState.value.memory ?: return
        viewModelScope.launch {
            val session = preferencesRepository.userSessionFlow.first()
            memoryRepository.logicalDeleteMemory(memory.id,
                session.matricula)
                .onSuccess {
                    _events.emit(MemoryDetailEvent.DeletedAndNavigateBack) }
                .onFailure {
                    _events.emit(MemoryDetailEvent.ShowError(it.message ?: "Error al eliminar")) }
        }
    }
    fun setHistoryDialogVisible(visible: Boolean) {
        _showHistoryDialog.value = visible
    }
}