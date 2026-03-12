package com.cadnative.firevisioniptv.data.model

/**
 * A generic wrapper for operation results that can either succeed or fail.
 * 
 * This sealed class provides a type-safe way to handle success and failure cases
 * in repository and data source operations, making error handling explicit and
 * preventing null pointer exceptions.
 * 
 * @param T The type of data returned on success
 */
sealed class Result<out T> {
    /**
     * Represents a successful operation with data.
     * 
     * @param data The successful result data
     */
    data class Success<out T>(val data: T) : Result<T>()
    
    /**
     * Represents a failed operation with an error.
     * 
     * @param exception The exception that caused the failure
     */
    data class Error(val exception: Exception) : Result<Nothing>()
    
    /**
     * Returns true if this is a Success result.
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Returns true if this is an Error result.
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Returns the data if this is a Success, or null if this is an Error.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    /**
     * Returns the exception if this is an Error, or null if this is a Success.
     */
    fun exceptionOrNull(): Exception? = when (this) {
        is Success -> null
        is Error -> exception
    }
    
    companion object {
        /**
         * Factory method to create a Success result.
         */
        fun <T> success(data: T): Result<T> = Success(data)
        
        /**
         * Factory method to create an Error result.
         */
        fun error(exception: Exception): Result<Nothing> = Error(exception)
    }
}
