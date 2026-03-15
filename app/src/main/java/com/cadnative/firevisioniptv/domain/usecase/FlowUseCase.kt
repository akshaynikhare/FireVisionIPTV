package com.cadnative.firevisioniptv.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Abstract base class for use cases that perform streaming operations.
 *
 * Use cases encapsulate business logic and orchestrate data flow between
 * the presentation layer and the data layer. This base class provides a
 * consistent pattern for executing operations that return a stream of results
 * using Kotlin Flow.
 *
 * FlowUseCase is ideal for operations that:
 * - Emit multiple values over time
 * - Need to observe data changes (e.g., database queries)
 * - Implement reactive patterns
 *
 * @param P The type of parameters required by the use case
 * @param R The type of result emitted by the Flow
 *
 * Example usage:
 * ```
 * class GetChannelsUseCase @Inject constructor(
 *     private val channelRepository: ChannelRepository
 * ) : FlowUseCase<Unit, Result<List<Channel>>>() {
 *     override fun execute(params: Unit): Flow<Result<List<Channel>>> {
 *         return channelRepository.getChannels()
 *     }
 * }
 * ```
 *
 * Requirements: TR-003 (Clean Architecture with Use Cases)
 */
abstract class FlowUseCase<in P, out R> {
    
    /**
     * Invoke operator allows the use case to be called like a function.
     *
     * @param params The parameters required by the use case
     * @return A Flow that emits the results of the use case execution
     */
    operator fun invoke(params: P): Flow<R> = execute(params)
    
    /**
     * Executes the business logic of the use case and returns a Flow.
     *
     * This method must be implemented by concrete use case classes to define
     * the specific business logic to be executed. The returned Flow will emit
     * results over time as they become available.
     *
     * @param params The parameters required by the use case
     * @return A Flow that emits the results of the use case execution
     */
    protected abstract fun execute(params: P): Flow<R>
}
