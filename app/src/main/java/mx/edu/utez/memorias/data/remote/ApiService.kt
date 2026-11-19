package mx.edu.utez.memorias.data.remote

import mx.edu.utez.memorias.data.remote.dto.AuthResponse
import mx.edu.utez.memorias.data.remote.dto.EditHistoryDto
import mx.edu.utez.memorias.data.remote.dto.LikeToggleRequest
import mx.edu.utez.memorias.data.remote.dto.LikeToggleResponse
import mx.edu.utez.memorias.data.remote.dto.LoginRequest
import mx.edu.utez.memorias.data.remote.dto.MemoryDto
import mx.edu.utez.memorias.data.remote.dto.PendingNotificationDto
import mx.edu.utez.memorias.data.remote.dto.RegisterRequest
import mx.edu.utez.memorias.data.remote.dto.UpdateMemoryRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest):
            Response<AuthResponse>
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse> @GET("api/memories")
    suspend fun getMemories(): Response<List<MemoryDto>>
    @Multipart
    @POST("api/memories")
    suspend fun createMemory(
        @Part("user_matricula") userMatricula: RequestBody,
        @Part("titulo") titulo: RequestBody,
        @Part("lugar") lugar: RequestBody,
        @Part("latitud") latitud: RequestBody,
        @Part("longitud") longitud: RequestBody,
        @Part("descripcion") descripcion: RequestBody,
        @Part("timestamp") timestamp: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<MemoryDto>
    @PUT("api/memories/{id}")
    suspend fun updateMemory(
        @Path("id") memoryId: Int,
        @Body request: UpdateMemoryRequest
    ): Response<MemoryDto>
    @DELETE("api/memories/{id}")
    suspend fun deleteMemory(
        @Path("id") memoryId: Int,
        @Query("user_matricula") userMatricula: String
    ): Response<Unit>
    @POST("api/memories/{id}/like")
    suspend fun toggleLike(
        @Path("id") memoryId: Int,
        @Body request: LikeToggleRequest
    ): Response<LikeToggleResponse>
    @GET("api/memories/{id}/history")
    suspend fun getEditHistory(@Path("id") memoryId: Int):
            Response<List<EditHistoryDto>>
    @GET("api/notifications/pending/{matricula}")
    suspend fun getPendingNotifications(
        @Path("matricula") matricula: String
    ): Response<List<PendingNotificationDto>>
}