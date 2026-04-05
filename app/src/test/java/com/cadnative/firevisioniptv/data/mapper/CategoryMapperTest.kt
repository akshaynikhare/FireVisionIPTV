package com.cadnative.firevisioniptv.data.mapper

import com.cadnative.firevisioniptv.data.model.dto.CategoryDto
import com.cadnative.firevisioniptv.data.source.local.entity.CategoryEntity
import com.cadnative.firevisioniptv.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CategoryMapperTest {

    private lateinit var mapper: CategoryMapper

    @Before
    fun setUp() {
        mapper = CategoryMapper()
    }

    @Test
    fun `toDomain maps entity fields to category`() {
        val entity = CategoryEntity(id = "cat1", name = "News", displayOrder = 2, channelCount = 10)

        val category = mapper.toDomain(entity)

        assertEquals("cat1", category.id)
        assertEquals("News", category.name)
        assertEquals(10, category.channelCount)
    }

    @Test
    fun `toEntity maps dto fields to entity`() {
        val dto = CategoryDto(id = "cat1", name = "Sports", displayOrder = 1, channelCount = 5)

        val entity = mapper.toEntity(dto)

        assertEquals("cat1", entity.id)
        assertEquals("Sports", entity.name)
        assertEquals(1, entity.displayOrder)
        assertEquals(5, entity.channelCount)
    }

    @Test
    fun `fromDomain sets displayOrder to zero`() {
        val category = Category(id = "cat1", name = "News", channelCount = 3)

        val entity = mapper.fromDomain(category)

        assertEquals(0, entity.displayOrder)
    }

    @Test
    fun `fromDomain preserves channelCount`() {
        val category = Category(id = "cat1", name = "News", channelCount = 42)

        val entity = mapper.fromDomain(category)

        assertEquals(42, entity.channelCount)
    }

    @Test
    fun `round trip toDomain then fromDomain preserves id name and channelCount`() {
        val entity = CategoryEntity(id = "cat1", name = "Entertainment", displayOrder = 3, channelCount = 7)

        val roundTripped = mapper.fromDomain(mapper.toDomain(entity))

        assertEquals(entity.id, roundTripped.id)
        assertEquals(entity.name, roundTripped.name)
        assertEquals(entity.channelCount, roundTripped.channelCount)
    }
}
