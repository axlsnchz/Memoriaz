package mx.edu.utez.memorias.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "edit_history",
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["memory_id"])]
)
data class EditHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "memory_id")
    val memoryId: Int,
    @ColumnInfo(name = "edited_at")
    val editedAt: String,
    @ColumnInfo(name = "change_summary")
    val changeSummary: String
)