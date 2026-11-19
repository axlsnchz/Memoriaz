package mx.edu.utez.memorias.data.repository

import kotlinx.coroutines.flow.Flow
import mx.edu.utez.memorias.data.local.UserPreferencesRepository
import mx.edu.utez.memorias.data.local.UserSession
import mx.edu.utez.memorias.data.local.dao.UserDao
import mx.edu.utez.memorias.data.local.entity.UserEntity
import mx.edu.utez.memorias.data.remote.ApiService
import mx.edu.utez.memorias.data.remote.dto.LoginRequest
import mx.edu.utez.memorias.data.remote.dto.RegisterRequest
import java.security.MessageDigest
import java.time.Instant
class AuthRepository(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val preferencesRepository: UserPreferencesRepository
) {
    val sessionFlow: Flow<UserSession> =
        preferencesRepository.userSessionFlow
    suspend fun login(matricula: String, password: String):
            Result<UserSession> {
        val cleanMatricula = matricula.trim()
        return try {
            val response = apiService.login(LoginRequest(cleanMatricula, password))
            if (response.isSuccessful && response.body()?.success == true && response.body()?.user != null) {
                val user = response.body()!!.user!!
                userDao.insertUser(
                    UserEntity(
                        matricula = user.matricula,
                        nombres = user.nombres,
                        apellidos = user.apellidos,
                        passwordHash = hashPassword(password),
                        createdAt = user.createdAt ?:
                        Instant.now().toString()
                    )
                )
                preferencesRepository.saveSession(user.matricula,
                    user.nombres, user.apellidos)
                Result.success(UserSession(user.matricula, user.nombres, user.apellidos, true))
            } else {
                Result.failure(Exception(response.body()?.message ?:
                "Credenciales inválidas"))
            }
        } catch (e: Exception) {
            // Respaldo Offline: validar contra caché local en Room
            val localUser = userDao.getUserByMatricula(cleanMatricula)
            if (localUser != null && localUser.passwordHash ==
                hashPassword(password)) {
                preferencesRepository.saveSession(localUser.matricula,
                    localUser.nombres, localUser.apellidos)
                Result.success(UserSession(localUser.matricula,
                    localUser.nombres, localUser.apellidos, true))
            } else {
                Result.failure(Exception("Sin conexión o usuario no registrado localmente"))
            }
        }
    }

    private fun register(registerRequest: RegisterRequest) {}

    suspend fun register(
        matricula: String,
        nombres: String,
        apellidos: String,
        password: String
    ): Result<UserSession> {
        val cleanMatricula = matricula.trim()
        val now = Instant.now().toString()
        val passwordHash = hashPassword(password)
        return try {
            val response = apiService.register(
                RegisterRequest(
                    cleanMatricula, nombres.trim(),
                    apellidos.trim(), password
                )
            )
            if (response.isSuccessful && response.body()?.success == true) { userDao.insertUser(
                UserEntity(
                    cleanMatricula, nombres.trim(),
                    apellidos.trim(), passwordHash, now
                )
            )
                preferencesRepository.saveSession(cleanMatricula,
                    nombres.trim(), apellidos.trim())
                Result.success(UserSession(cleanMatricula, nombres.trim(), apellidos.trim(), true))
            } else {
                Result.failure(Exception(response.body()?.message ?: "La matrícula ya existe"))
            }
        } catch (e: Exception) {
            // Registro local si no hay servidor disponible
            if (userDao.getUserByMatricula(cleanMatricula) != null) {
                return Result.failure(Exception("La matrícula ya está registrada localmente"))
            }
            userDao.insertUser(
                UserEntity(cleanMatricula, nombres.trim(), apellidos.trim(), passwordHash, now)
            )
            preferencesRepository.saveSession(cleanMatricula,
                nombres.trim(), apellidos.trim())
            Result.success(UserSession(cleanMatricula, nombres.trim(),
                apellidos.trim(), true))
        }
    }
    suspend fun logout() {
        preferencesRepository.clearSession()
    }
    private fun hashPassword(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}