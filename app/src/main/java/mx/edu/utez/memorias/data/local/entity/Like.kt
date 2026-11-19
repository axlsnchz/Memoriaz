package mx.edu.utez.memorias.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "likes",
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["matricula"],
            childColumns = ["liker_matricula"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["memory_id"]),
        Index(value = ["liker_matricula"]),
        Index(value = ["memory_id", "liker_matricula"], unique = true)
    ]
)
data class LikeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "memory_id")
    val memoryId: Int,
    @ColumnInfo(name = "liker_matricula")
    val likerMatricula: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "notified")
    val notified: Int = 0
)
