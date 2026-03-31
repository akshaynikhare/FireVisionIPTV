package com.cadnative.firevisioniptv.data.source.remote

import com.cadnative.firevisioniptv.data.model.dto.CategoriesResponse
import com.cadnative.firevisioniptv.data.model.dto.ChannelDto
import com.cadnative.firevisioniptv.data.model.dto.ChannelsResponse
import com.cadnative.firevisioniptv.data.model.dto.EpgGuideResponse
import com.cadnative.firevisioniptv.data.model.dto.FavoritesRequest
import com.cadnative.firevisioniptv.data.model.dto.FavoritesResponse
import com.cadnative.firevisioniptv.data.model.dto.HealthSyncRequest
import com.cadnative.firevisioniptv.data.model.dto.StreamPlayReport
import com.cadnative.firevisioniptv.data.model.dto.StreamStatusReport
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API service interface for FireVision IPTV backend.
 * 
 * This interface defines all API endpoints for the FireVision IPTV application.
 * All methods return Response<T> to enable proper error handling at the repository layer.
 * 
 * Requirements: TR-002 (Update Dependencies - Retrofit 2.11.0)
 */
interface FireVisionApiService {
    
    /**
     * Fetches all channels from the server.
     * 
     * @return Response containing ChannelsResponse with list of channels and metadata
     */
    @GET("api/v1/channels")
    suspend fun getChannels(): Response<ChannelsResponse>
    
    /**
     * Fetches a specific channel by its ID.
     * 
     * @param id The unique identifier of the channel
     * @return Response containing the ChannelDto for the requested channel
     */
    @GET("api/v1/channels/{id}")
    suspend fun getChannelById(@Path("id") id: String): Response<ChannelDto>
    
    /**
     * Fetches all categories from the server.
     * 
     * @return Response containing CategoriesResponse with list of categories and metadata
     */
    @GET("api/v1/categories")
    suspend fun getCategories(): Response<CategoriesResponse>
    
    /**
     * Syncs user favorites to the server.
     * 
     * This endpoint allows the app to synchronize favorite channels across devices.
     * The server will store the favorites associated with the device ID.
     * 
     * @param favorites FavoritesRequest containing channel IDs and device information
     * @return Response with Unit on success
     */
    @POST("api/v1/favorites")
    suspend fun syncFavorites(@Body favorites: FavoritesRequest): Response<Unit>
    
    /**
     * Fetches the M3U playlist from the server.
     * 
     * This endpoint returns the raw M3U playlist file which can be parsed
     * to extract channel information.
     * 
     * @return Response containing ResponseBody with the M3U playlist content
     */
    @GET("api/v1/channels/playlist.m3u")
    suspend fun getPlaylist(): Response<ResponseBody>

    @GET("api/v1/favorites")
    suspend fun getFavorites(): Response<FavoritesResponse>

    @POST("api/v1/channels/{channelId}/report-status")
    suspend fun reportStreamStatus(
        @Path("channelId") channelId: String,
        @Body report: StreamStatusReport
    ): Response<Unit>

    @POST("api/v1/channels/{channelId}/report-play")
    suspend fun reportStreamPlay(
        @Path("channelId") channelId: String,
        @Body report: StreamPlayReport
    ): Response<Unit>

    @POST("api/v1/channels/health-sync")
    suspend fun syncHealthResults(
        @Body request: HealthSyncRequest
    ): Response<Unit>

    @GET("tv/epg/{channelListCode}/json")
    suspend fun getEpgGuide(
        @Path("channelListCode") channelListCode: String,
        @Query("hours") hours: Int = 12
    ): Response<EpgGuideResponse>
}
