package mx.edu.utez.memorias.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import mx.edu.utez.memorias.data.local.entity.MemoryEntity

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun observeAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE user_matricula = :matricula ORDER BY timestamp DESC")
    fun observeMemoriesByUser(matricula: String): Flow<List<MemoryEntity>> @Query("SELECT * FROM memories WHERE id = :memoryId LIMIT 1")

    fun observeMemoryById(memoryId: Int): Flow<MemoryEntity?>

    @Query("SELECT * FROM memories WHERE id = :memoryId LIMIT 1")
    suspend fun getMemoryById(memoryId: Int): MemoryEntity?

    @Query("SELECT * FROM memories WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getMemoryByRemoteId(remoteId: Int): MemoryEntity?

    @Query("SELECT * FROM memories WHERE is_synced = 0")
    suspend fun getUnsyncedMemories(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("UPDATE memories SET is_deleted = 1, is_synced = :isSynced WHERE id = :memoryId")
    suspend fun logicalDeleteMemory(memoryId: Int, isSynced: Boolean)
}
