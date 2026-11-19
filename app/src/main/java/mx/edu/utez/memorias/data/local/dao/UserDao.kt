package mx.edu.utez.memorias.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.edu.utez.memorias.data.local.entity.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserIgnore(user: UserEntity)
    @Query("SELECT * FROM users WHERE matricula = :matricula LIMIT 1")
    suspend fun getUserByMatricula(matricula: String): UserEntity?
}