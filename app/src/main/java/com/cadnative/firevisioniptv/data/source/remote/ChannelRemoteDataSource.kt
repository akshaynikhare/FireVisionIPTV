package com.cadnative.firevisioniptv.data.source.remote

import com.cadnative.firevisioniptv.data.model.Result
import com.cadnative.firevisioniptv.data.model.dto.ChannelDto
import com.cadnative.firevisioniptv.data.model.dto.ChannelsResponse
import com.cadnative.firevisioniptv.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source for channel-related API operations.
 * 
 * This class wraps the FireVisionApiService and provides error handling
 * for all channel-related network operations. It converts API responses
 * into Result<T> objects for consistent error handling throughout the app.
 * 
 * Requirements: TR-006 (Network Performance - API response caching, error handling)
 */
@Singleton
class ChannelRemoteDataSource @Inject constructor(
    private val apiService: FireVisionApiService,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    
    /**
     * Fetches all channels from the remote API.
     * 
     * @return Result containing list of ChannelDto on success, or error on failure
     */
    suspend fun fetchChannels(): Result<List<ChannelDto>> = withContext(dispatcher) {
        try {
            val response = apiService.getChannels()
            handleResponse(response) { it.data }
        } catch (e: IOException) {
            Result.Error(
                NetworkException("Network error: ${e.message}", e)
            )
        } catch (e: HttpException) {
            Result.Error(
                toAppException(e.code(), e.message())
            )
        } catch (e: Exception) {
            Result.Error(
                UnknownException("Unexpected error: ${e.message}", e)
            )
        }
    }
    
    /**
     * Fetches a specific channel by ID from the remote API.
     * 
     * @param channelId The unique identifier of the channel
     * @return Result containing ChannelDto on success, or error on failure
     */
    suspend fun fetchChannelById(channelId: String): Result<ChannelDto> = withContext(dispatcher) {
        try {
            val response = apiService.getChannelById(channelId)
            handleResponse(response) { it }
        } catch (e: IOException) {
            Result.Error(
                NetworkException("Network error: ${e.message}", e)
            )
        } catch (e: HttpException) {
            Result.Error(
                toAppException(e.code(), e.message())
            )
        } catch (e: Exception) {
            Result.Error(
                UnknownException("Unexpected error: ${e.message}", e)
            )
        }
    }
    
    /**
     * Handles API response and converts it to Result.
     */
    private fun <T, R> handleResponse(
        response: Response<T>,
        transform: (T) -> R
    ): Result<R> {
        return if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.Success(transform(body))
            } else {
                Result.Error(Exception("Response body is null"))
            }
        } else {
            Result.Error(
                toAppException(response.code(), response.message())
            )
        }
    }
    
    /**
     * Converts HTTP status code to a specific app exception.
     */
    private fun toAppException(code: Int, message: String): Exception {
        return when (code) {
            400 -> BadRequestException("Bad request: $message")
            401 -> UnauthorizedException("Unauthorized: $message")
            403 -> ForbiddenException("Forbidden: $message")
            404 -> NotFoundException("Not found: $message")
            500 -> ServerException("Server error: $message")
            503 -> ServiceUnavailableException("Service unavailable: $message")
            else -> Exception("HTTP error $code: $message")
        }
    }
}

/**
 * Exception thrown when a network error occurs (no internet, timeout, etc.).
 */
class NetworkException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Exception thrown when an unexpected error occurs.
 */
class UnknownException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown for 400 Bad Request responses.
 */
class BadRequestException(message: String) : Exception(message)

/**
 * Exception thrown for 401 Unauthorized responses.
 */
class UnauthorizedException(message: String) : Exception(message)

/**
 * Exception thrown for 403 Forbidden responses.
 */
class ForbiddenException(message: String) : Exception(message)

/**
 * Exception thrown for 404 Not Found responses.
 */
class NotFoundException(message: String) : Exception(message)

/**
 * Exception thrown for 500 Internal Server Error responses.
 */
class ServerException(message: String) : Exception(message)

/**
 * Exception thrown for 503 Service Unavailable responses.
 */
class ServiceUnavailableException(message: String) : Exception(message)
