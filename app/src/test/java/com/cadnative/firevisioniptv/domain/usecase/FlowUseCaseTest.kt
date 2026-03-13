package com.cadnative.firevisioniptv.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the FlowUseCase base class.
 *
 * Tests verify that:
 * - The invoke operator correctly delegates to execute()
 * - Flow emissions work correctly
 * - Multiple emissions are handled properly
 * - Concrete implementations can return various Flow types
 */
class FlowUseCaseTest {
    
    @Test
    fun `invoke operator calls execute method and returns Flow`() = runTest {
        // Given
        val expectedValue = "test result"
        val useCase = TestFlowUseCase(expectedValue)
        
        // When
        val result = useCase("test param").first()
        
        // Then
        assertEquals(expectedValue, result)
    }
    
    @Test
    fun `flow use case with Unit parameter works correctly`() = runTest {
        // Given
        val expectedValue = 42
        val useCase = TestFlowUseCaseWithUnitParam(expectedValue)
        
        // When
        val result = useCase(Unit).first()
        
        // Then
        assertEquals(expectedValue, result)
    }
    
    @Test
    fun `flow use case emits multiple values correctly`() = runTest {
        // Given
        val expectedValues = listOf(1, 2, 3, 4, 5)
        val useCase = object : FlowUseCase<Unit, Int>() {
            override fun execute(params: Unit): Flow<Int> = flow {
                expectedValues.forEach { emit(it) }
            }
        }
        
        // When
        val results = useCase(Unit).toList()
        
        // Then
        assertEquals(expectedValues, results)
    }
    
    @Test
    fun `flow use case with complex parameter works correctly`() = runTest {
        // Given
        data class ComplexParam(val count: Int)
        val param = ComplexParam(3)
        val useCase = object : FlowUseCase<ComplexParam, String>() {
            override fun execute(params: ComplexParam): Flow<String> = flow {
                repeat(params.count) { index ->
                    emit("Item $index")
                }
            }
        }
        
        // When
        val results = useCase(param).toList()
        
        // Then
        assertEquals(listOf("Item 0", "Item 1", "Item 2"), results)
    }
    
    @Test
    fun `flow use case with Result wrapper works correctly`() = runTest {
        // Given
        val expectedValue = "success"
        val useCase = object : FlowUseCase<Unit, Result<String>>() {
            override fun execute(params: Unit): Flow<Result<String>> = flow {
                emit(Result.success(expectedValue))
            }
        }
        
        // When
        val result = useCase(Unit).first()
        
        // Then
        assertEquals(true, result.isSuccess)
        assertEquals(expectedValue, result.getOrNull())
    }
    
    @Test
    fun `flow use case can emit success and failure results`() = runTest {
        // Given
        val useCase = object : FlowUseCase<Unit, Result<Int>>() {
            override fun execute(params: Unit): Flow<Result<Int>> = flow {
                emit(Result.success(1))
                emit(Result.success(2))
                emit(Result.failure(RuntimeException("Error")))
                emit(Result.success(3))
            }
        }
        
        // When
        val results = useCase(Unit).toList()
        
        // Then
        assertEquals(4, results.size)
        assertEquals(true, results[0].isSuccess)
        assertEquals(1, results[0].getOrNull())
        assertEquals(true, results[1].isSuccess)
        assertEquals(2, results[1].getOrNull())
        assertEquals(true, results[2].isFailure)
        assertEquals(true, results[3].isSuccess)
        assertEquals(3, results[3].getOrNull())
    }
    
    @Test
    fun `flow use case emits empty flow correctly`() = runTest {
        // Given
        val useCase = object : FlowUseCase<Unit, String>() {
            override fun execute(params: Unit): Flow<String> = flow {
                // Empty flow - no emissions
            }
        }
        
        // When
        val results = useCase(Unit).toList()
        
        // Then
        assertEquals(emptyList<String>(), results)
    }
    
    // Test implementations
    private class TestFlowUseCase(
        private val result: String
    ) : FlowUseCase<String, String>() {
        override fun execute(params: String): Flow<String> = flow {
            emit(result)
        }
    }
    
    private class TestFlowUseCaseWithUnitParam(
        private val result: Int
    ) : FlowUseCase<Unit, Int>() {
        override fun execute(params: Unit): Flow<Int> = flow {
            emit(result)
        }
    }
}
