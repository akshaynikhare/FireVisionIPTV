package com.cadnative.firevisioniptv.presentation.mapper

import com.cadnative.firevisioniptv.domain.model.Category
import com.cadnative.firevisioniptv.presentation.model.CategoryUiModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CategoryUiMapperTest {

    private lateinit var mapper: CategoryUiMapper

    @Before
    fun setup() {
        mapper = CategoryUiMapper()
    }

    @Test
    fun `toUiModel maps all fields`() {
        val category = Category(id = "cat1", name = "Sports", channelCount = 42)
        val result = mapper.toUiModel(category)

        assertEquals("cat1", result.id)
        assertEquals("Sports", result.name)
        assertEquals(42, result.channelCount)
    }

    @Test
    fun `fromUiModel maps all fields`() {
        val uiModel = CategoryUiModel(id = "cat2", name = "News", channelCount = 10)
        val result = mapper.fromUiModel(uiModel)

        assertEquals("cat2", result.id)
        assertEquals("News", result.name)
        assertEquals(10, result.channelCount)
    }

    @Test
    fun `round trip preserves data`() {
        val original = Category(id = "cat3", name = "Entertainment", channelCount = 100)
        val roundTripped = mapper.fromUiModel(mapper.toUiModel(original))

        assertEquals(original, roundTripped)
    }

    @Test
    fun `toUiModel handles zero channel count`() {
        val category = Category(id = "empty", name = "Empty", channelCount = 0)
        val result = mapper.toUiModel(category)

        assertEquals(0, result.channelCount)
    }
}
