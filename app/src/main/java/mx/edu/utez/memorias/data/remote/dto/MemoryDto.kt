package mx.edu.utez.memorias.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MemoryDto(
    val id: Int,
    @SerializedName("user_matricula") val userMatricula: String,
    @SerializedName("author_name") val authorName: String? = null,
    val titulo: String,
    val lugar: String,
    val latitud: Double,
    val longitud: Double,
    val descripcion: String,
    @SerializedName("image_filename") val imageFilename: String? = null,
    val timestamp: String,
    @SerializedName("is_deleted") val isDeleted: Int = 0,
    @SerializedName("likes_count") val likesCount: Int = 0
)
data class UpdateMemoryRequest(
    val titulo: String,
    val lugar: String,
    val latitud: Double,
    val longitud: Double,
    val descripcion: String,
    @SerializedName("change_summary") val changeSummary: String
)
data class LikeToggleRequest(
    @SerializedName("liker_matricula") val likerMatricula: String
)
data class LikeToggleResponse(
    val liked: Boolean,
    @SerializedName("likes_count") val likesCount: Int
)
data class EditHistoryDto(
    val id: Int,
    @SerializedName("memory_id") val memoryId: Int,
    @SerializedName("edited_at") val editedAt: String,
    @SerializedName("change_summary") val changeSummary: String
)