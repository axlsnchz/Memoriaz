package mx.edu.utez.memorias.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PendingNotificationDto(
    val id: Int,
    @SerializedName("memory_id") val memoryId: Int,
    @SerializedName("memory_titulo") val memoryTitulo: String,
    @SerializedName("liker_matricula") val likerMatricula: String,
    @SerializedName("liker_name") val likerName: String,
    @SerializedName("created_at") val createdAt: String
)