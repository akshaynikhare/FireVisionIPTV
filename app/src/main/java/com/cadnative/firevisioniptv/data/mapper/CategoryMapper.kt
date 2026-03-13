package com.cadnative.firevisioniptv.data.mapper

import com.cadnative.firevisioniptv.data.model.dto.CategoryDto
import com.cadnative.firevisioniptv.data.source.local.entity.CategoryEntity
import com.cadnative.firevisioniptv.domain.model.Category
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for converting between Category representations across layers.
 * 
 * This mapper handles bidirectional transformations between:
 * - Domain models (Category) used in business logic
 * - Database entities (CategoryEntity) used for local storage
 * - DTOs (CategoryDto) used for network communication
 * 
 * Requirements: TR-003 (Clean Architecture - proper layer separation)
 */
@Singleton
class CategoryMapper @Inject constructor() {
    
    /**
     * Convert a CategoryEntity to a domain Category model.
     * 
     * @param entity The database entity
     * @return Domain model representation
     */
    fun toDomain(entity: CategoryEntity): Category {
        return Category(
            id = entity.id,
            name = entity.name,
            channelCount = entity.channelCount
        )
    }
    
    /**
     * Convert a CategoryDto to a CategoryEntity for local storage.
     * 
     * @param dto The data transfer object from API
     * @return Database entity representation
     */
    fun toEntity(dto: CategoryDto): CategoryEntity {
        return CategoryEntity(
            id = dto.id,
            name = dto.name,
            displayOrder = dto.displayOrder,
            channelCount = dto.channelCount
        )
    }
    
    /**
     * Convert a domain Category model to a CategoryEntity.
     * 
     * @param category The domain model
     * @return Database entity representation
     */
    fun fromDomain(category: Category): CategoryEntity {
        return CategoryEntity(
            id = category.id,
            name = category.name,
            displayOrder = 0,
            channelCount = category.channelCount
        )
    }
}
