package mx.edu.utez.memorias.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import mx.edu.utez.memorias.data.local.dao.EditHistoryDao
import mx.edu.utez.memorias.data.local.dao.LikeDao
import mx.edu.utez.memorias.data.local.dao.MemoryDao
import mx.edu.utez.memorias.data.local.dao.UserDao
import mx.edu.utez.memorias.data.local.entity.EditHistoryEntity
import mx.edu.utez.memorias.data.local.entity.LikeEntity
import mx.edu.utez.memorias.data.local.entity.MemoryEntity
import mx.edu.utez.memorias.data.local.entity.UserEntity
import mx.edu.utez.memorias.data.remote.ApiService
import mx.edu.utez.memorias.data.remote.dto.LikeToggleRequest
import mx.edu.utez.memorias.data.remote.dto.UpdateMemoryRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant

data class MemoryUiModel(
    val entity: MemoryEntity,
    val likesCount: Int,
    val isLikedByCurrentUser: Boolean
)
class MemoryRepository(
    private val apiService: ApiService,
    private val memoryDao: MemoryDao,
    private val userDao: UserDao,
    private val likeDao: LikeDao,
    private val editHistoryDao: EditHistoryDao
) {
    fun observeAllMemoriesWithLikes(currentMatricula: String):
            Flow<List<MemoryUiModel>> {
        return memoryDao.observeAllMemories().combine(
            kotlinx.coroutines.flow.flowOf(currentMatricula)
        ) { memories, matricula ->
            memories.map { memory ->
                MemoryUiModel(
                    entity = memory,
                    likesCount = 0, // Enriquecido dinámicamente en detalle o vía tabla likes
                            isLikedByCurrentUser = false
                )
            }
        }
    }
    fun observeAllMemories(): Flow<List<MemoryEntity>> =
        memoryDao.observeAllMemories()
    fun observeUserMemories(matricula: String): Flow<List<MemoryEntity>> = memoryDao.observeMemoriesByUser(matricula)
    fun observeMemoryById(memoryId: Int): Flow<MemoryEntity?> =
        memoryDao.observeMemoryById(memoryId)
    fun observeLikeCount(memoryId: Int): Flow<Int> =
        likeDao.observeLikeCount(memoryId)
    fun observeIsLikedByUser(memoryId: Int, matricula: String):
            Flow<Boolean> =
        likeDao.observeIsLikedByUser(memoryId, matricula)
    fun observeEditHistory(memoryId: Int): Flow<List<EditHistoryEntity>> = editHistoryDao.observeHistoryForMemory(memoryId)
    suspend fun getMemoryById(memoryId: Int): MemoryEntity? =
        memoryDao.getMemoryById(memoryId)
    suspend fun refreshMemoriesFromRemote(): Result<Unit> {
        return try {
            val response = apiService.getMemories()
            if (response.isSuccessful && response.body() != null) {
                val remoteList = response.body()!!
                for (dto in remoteList) {
                    // Garantizar integridad referencial (Foreign Key user_matricula -> users.matricula)
                    userDao.insertUserIgnore(
                        UserEntity(
                            matricula = dto.userMatricula,
                            nombres = dto.authorName ?: dto.userMatricula, apellidos = "",
                            passwordHash = "",
                            createdAt = dto.timestamp
                        )
                    )
                    val existingLocal =
                        memoryDao.getMemoryByRemoteId(dto.id)
                            ?: memoryDao.getMemoryById(dto.id)
                    val entity = MemoryEntity(
                        id = existingLocal?.id ?: dto.id,
                        userMatricula = dto.userMatricula,
                        titulo = dto.titulo,
                        lugar = dto.lugar,
                        latitud = dto.latitud,
                        longitud = dto.longitud,
                        descripcion = dto.descripcion,
                        imageFilename = dto.imageFilename,
                        timestamp = dto.timestamp,
                        isDeleted = dto.isDeleted,
                        isSynced = true,
                        localImagePath = existingLocal?.localImagePath,
                        remoteId = dto.id
                    )
                    memoryDao.insertMemory(entity)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error HTTP ${response.code()}")) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun createMemory(
        userMatricula: String,
        titulo: String,
        lugar: String,
        latitud: Double,
        longitud: Double,
        descripcion: String,
        localImagePath: String?,
        captureTimestamp: String
    ): Result<Boolean> {
        // 1. Guardar inmediatamente en Room (Single Source of Truth)
        val localId = memoryDao.insertMemory(
            MemoryEntity(
                userMatricula = userMatricula,
                titulo = titulo,
                lugar = lugar,
                latitud = latitud,
                longitud = longitud,
                descripcion = descripcion,
                timestamp = captureTimestamp,
                isDeleted = 0,
                isSynced = false,
                localImagePath = localImagePath
            )
        ).toInt()
        // 2. Intentar subir inmediatamente a la API REST
        val savedEntity = memoryDao.getMemoryById(localId) ?: return Result.success(false)
        return uploadSingleMemory(savedEntity)
    }
    suspend fun uploadSingleMemory(memory: MemoryEntity): Result<Boolean>{
        return try {
            val textPlain = "text/plain".toMediaTypeOrNull()
            val imagePart = memory.localImagePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val reqFile =
                        file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("image", file.name, reqFile)
                } else null
            }
            val response = apiService.createMemory(
                userMatricula =
                    memory.userMatricula.toRequestBody(textPlain),
                titulo = memory.titulo.toRequestBody(textPlain),
                lugar = memory.lugar.toRequestBody(textPlain),
                latitud =
                    memory.latitud.toString().toRequestBody(textPlain),
                longitud =
                    memory.longitud.toString().toRequestBody(textPlain),
                descripcion = memory.descripcion.toRequestBody(textPlain), timestamp = memory.timestamp.toRequestBody(textPlain),
                image = imagePart
            )
            if (response.isSuccessful && response.body() != null) {
                val remoteDto = response.body()!!
                memoryDao.updateMemory(
                    memory.copy(
                        imageFilename = remoteDto.imageFilename,
                        isSynced = true,
                        remoteId = remoteDto.id
                    )
                )
                Result.success(true) // Sincronizado online
            } else {
                Result.success(false) // Guardado offline pendiente
            }
        } catch (e: Exception) {
            Result.success(false) // Guardado offline pendiente para WorkManager
        }
    }
    suspend fun syncPendingOfflineMemories(): Int {
        val unsynced = memoryDao.getUnsyncedMemories()
        var syncedCount = 0
        for (memory in unsynced) {
            val result = uploadSingleMemory(memory)
            if (result.getOrDefault(false)) {
                syncedCount++
            }
        }
        return syncedCount
    }
    suspend fun updateMemory(
        memoryId: Int,
        userMatricula: String? = null,
        titulo: String,
        lugar: String,
        latitud: Double,
        longitud: Double,
        descripcion: String
    ): Result<Unit> {
        val current = memoryDao.getMemoryById(memoryId)
            ?: return Result.failure(Exception("Recuerdo no encontrado"))
        val matricula = userMatricula ?: current.userMatricula
        val changes = buildList {
            if (current.titulo != titulo) add("Título actualizado")
            if (current.lugar != lugar) add("Lugar actualizado a $lugar")
            if (current.latitud != latitud || current.longitud != longitud) add("Ubicación ajustada en mapa")
            if (current.descripcion != descripcion) add("Descripción modificada")
        }.ifEmpty { listOf("Edición general del recuerdo") }.joinToString(" • ")
        val editedAt = Instant.now().toString()
        // Actualizar en Room y registrar en edit_history
        memoryDao.updateMemory(
            current.copy(
                titulo = titulo,
                lugar = lugar,
                latitud = latitud,
                longitud = longitud,
                descripcion = descripcion,
                isSynced = false
            )
        )
        editHistoryDao.insertHistory(
            EditHistoryEntity(
                memoryId = memoryId,
                editedAt = editedAt,
                changeSummary = changes
            )
        )
        // Sincronizar con backend si tiene remoteId
        val targetRemoteId = current.remoteId ?: current.id
        try {
            val resp = apiService.updateMemory(
                targetRemoteId,
                matricula,
                UpdateMemoryRequest(
                    userMatricula = matricula,
                    titulo = titulo,
                    lugar = lugar,
                    latitud = latitud,
                    longitud = longitud,
                    descripcion = descripcion,
                    changeSummary = changes
                )
            )
            if (resp.isSuccessful) {
                memoryDao.updateMemory(
                    current.copy(
                        titulo = titulo,
                        lugar = lugar,
                        latitud = latitud,
                        longitud = longitud,
                        descripcion = descripcion,
                        isSynced = true
                    )
                )
            }
        } catch (_: Exception) {
            // Permanece guardado localmente
        }
        return Result.success(Unit)
    }
    suspend fun toggleLike(memoryId: Int, likerMatricula: String) {
        val existing = likeDao.getLike(memoryId, likerMatricula)
        if (existing != null) {
            likeDao.deleteLike(memoryId, likerMatricula)
        } else {
            likeDao.insertLike(
                LikeEntity(
                    memoryId = memoryId,
                    likerMatricula = likerMatricula,
                    createdAt = Instant.now().toString(),
                    notified = 0
                )
            )
        }
        val memory = memoryDao.getMemoryById(memoryId)
        val remoteId = memory?.remoteId ?: memoryId
        try {
            apiService.toggleLike(remoteId,
                LikeToggleRequest(likerMatricula)
            )
        } catch (_: Exception) {
            // El estado local ya se actualizó reactivamente
        }
    }
    suspend fun logicalDeleteMemory(memoryId: Int, currentUserMatricula:
    String): Result<Unit> {
        val memory = memoryDao.getMemoryById(memoryId)
            ?: return Result.failure(Exception("Recuerdo no encontrado"))
        if (memory.userMatricula != currentUserMatricula) {
            return Result.failure(Exception("Solo el autor puede eliminar este recuerdo"))
        }
        memoryDao.logicalDeleteMemory(memoryId, isSynced = false)
        val remoteId = memory.remoteId ?: memoryId
        try {
            val resp = apiService.deleteMemory(remoteId,
                currentUserMatricula)
            if (resp.isSuccessful) {
                memoryDao.logicalDeleteMemory(memoryId, isSynced = true)
            }
        } catch (_: Exception) {
            // Queda marcado lógicamente en Room
        }
        return Result.success(Unit)
    }
    suspend fun refreshEditHistory(memoryId: Int) {
        val memory = memoryDao.getMemoryById(memoryId) ?: return
        val remoteId = memory.remoteId ?: memoryId
        try {
            val resp = apiService.getEditHistory(remoteId)
            if (resp.isSuccessful && resp.body() != null) {
                resp.body()!!.forEach { dto ->
                    editHistoryDao.insertHistory(
                        EditHistoryEntity(
                            id = dto.id,
                            memoryId = memoryId,
                            editedAt = dto.editedAt,
                            changeSummary = dto.changeSummary
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
    }
}