package mx.edu.utez.memorias.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mx.edu.utez.memorias.data.local.entity.EditHistoryEntity

@Dao
interface EditHistoryDao {
    @Query("SELECT * FROM edit_history WHERE memory_id = :memoryId ORDER BY edited_at DESC")
    fun observeHistoryForMemory(memoryId: Int):
            Flow<List<EditHistoryEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: EditHistoryEntity)
}