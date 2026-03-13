package com.cadnative.firevisioniptv.domain.usecase

/**
 * Abstract base class for use cases that perform single-shot operations.
 *
 * Use cases encapsulate business logic and orchestrate data flow between
 * the presentation layer and the data layer. This base class provides a
 * consistent pattern for executing operations that return a single result.
 *
 * @param P The type of parameters required by the use case
 * @param R The type of result returned by the use case
 *
 * Example usage:
 * ```
 * class RefreshChannelsUseCase @Inject constructor(
 *     private val channelRepository: ChannelRepository
 * ) : UseCase<Unit, Result<Unit>>() {
 *     override suspend fun execute(params: Unit): Result<Unit> {
 *         return channelRepository.refreshChannels()
 *     }
 * }
 * ```
 *
 * Requirements: TR-003 (Clean Architecture with Use Cases)
 */
abstract class UseCase<in P, out R> {
    
    /**
     * Invoke operator allows the use case to be called like a function.
     *
     * @param params The parameters required by the use case
     * @return The result of the use case execution
     */
    suspend operator fun invoke(params: P): R = execute(params)
    
    /**
     * Executes the business logic of the use case.
     *
     * This method must be implemented by concrete use case classes to define
     * the specific business logic to be executed.
     *
     * @param params The parameters required by the use case
     * @return The result of the use case execution
     */
    protected abstract suspend fun execute(params: P): R
}
