package mx.edu.utez.memorias.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "matricula")
    val matricula: String,
    @ColumnInfo(name = "nombres")
    val nombres: String,
    @ColumnInfo(name = "apellidos")
    val apellidos: String,
    @ColumnInfo(name = "password_hash")
    val passwordHash: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)