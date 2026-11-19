package mx.edu.utez.memorias.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val matricula: String,
    val password: String
)
data class RegisterRequest(
    val matricula: String,
    val nombres: String,
    val apellidos: String,
    val password: String
)
data class UserDto(
    val matricula: String,
    val nombres: String,
    val apellidos: String,
    @SerializedName("password_hash") val passwordHash: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: UserDto? = null
)