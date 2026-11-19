package mx.edu.utez.memorias.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memories",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["matricula"],
            childColumns = ["user_matricula"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_matricula"])]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    @ColumnInfo(name = "user_matricula")
    val userMatricula: String,
    @ColumnInfo(name = "titulo")
    val titulo: String,
    @ColumnInfo(name = "lugar")
    val lugar: String,
    @ColumnInfo(name = "latitud")
    val latitud: Double,
    @ColumnInfo(name = "longitud")
    val longitud: Double,
    @ColumnInfo(name = "descripcion")
    val descripcion: String,
    @ColumnInfo(name = "image_filename")
    val imageFilename: String? = null,
    @ColumnInfo(name = "timestamp")
    val timestamp: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0, // 0 = activo, 1 = eliminado lógicamente
    // Banderas locales para soporte Offline-First
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = true,
    @ColumnInfo(name = "local_image_path")
    val localImagePath: String? = null,
    @ColumnInfo(name = "remote_id")
    val remoteId: Int? = null
)