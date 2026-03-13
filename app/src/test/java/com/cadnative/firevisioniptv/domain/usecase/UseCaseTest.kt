package com.cadnative.firevisioniptv.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the UseCase base class.
 *
 * Tests verify that:
 * - The invoke operator correctly delegates to execute()
 * - Concrete implementations can return various result types
 * - Suspend functions work correctly
 */
class UseCaseTest {
    
    @Test
    fun `invoke operator calls execute method`() = runTest {
        // Given
        val expectedResult = "test result"
        val useCase = TestUseCase(expectedResult)
        
        // When
        val result = useCase("test param")
        
        // Then
        assertEquals(expectedResult, result)
    }
    
    @Test
    fun `use case with Unit parameter works correctly`() = runTest {
        // Given
        val expectedResult = 42
        val useCase = TestUseCaseWithUnitParam(expectedResult)
        
        // When
        val result = useCase(Unit)
        
        // Then
        assertEquals(expectedResult, result)
    }
    
    @Test
    fun `use case with complex parameter works correctly`() = runTest {
        // Given
        data class ComplexParam(val value: Int, val name: String)
        val param = ComplexParam(10, "test")
        val useCase = object : UseCase<ComplexParam, String>() {
            override suspend fun execute(params: ComplexParam): String {
                return "${params.name}: ${params.value}"
            }
        }
        
        // When
        val result = useCase(param)
        
        // Then
        assertEquals("test: 10", result)
    }
    
    @Test
    fun `use case with Result wrapper works correctly`() = runTest {
        // Given
        val expectedValue = "success"
        val useCase = object : UseCase<Unit, Result<String>>() {
            override suspend fun execute(params: Unit): Result<String> {
                return Result.success(expectedValue)
            }
        }
        
        // When
        val result = useCase(Unit)
        
        // Then
        assertEquals(true, result.isSuccess)
        assertEquals(expectedValue, result.getOrNull())
    }
    
    @Test
    fun `use case can handle errors`() = runTest {
        // Given
        val expectedException = RuntimeException("Test error")
        val useCase = object : UseCase<Unit, Result<String>>() {
            override suspend fun execute(params: Unit): Result<String> {
                return Result.failure(expectedException)
            }
        }
        
        // When
        val result = useCase(Unit)
        
        // Then
        assertEquals(true, result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
    }
    
    // Test implementation
    private class TestUseCase(
        private val result: String
    ) : UseCase<String, String>() {
        override suspend fun execute(params: String): String = result
    }
    
    private class TestUseCaseWithUnitParam(
        private val result: Int
    ) : UseCase<Unit, Int>() {
        override suspend fun execute(params: Unit): Int = result
    }
}
