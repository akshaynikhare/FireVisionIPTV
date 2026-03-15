package com.cadnative.firevisioniptv.presentation.mapper

import com.cadnative.firevisioniptv.domain.model.Category
import com.cadnative.firevisioniptv.presentation.model.CategoryUiModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for converting between Category domain models and CategoryUiModel.
 * 
 * This mapper handles transformations between the domain layer and
 * presentation layer for category data.
 */
@Singleton
class CategoryUiMapper @Inject constructor() {
    
    /**
     * Convert a domain Category to a CategoryUiModel.
     */
    fun toUiModel(category: Category): CategoryUiModel {
        return CategoryUiModel(
            id = category.id,
            name = category.name,
            channelCount = category.channelCount
        )
    }
    
    /**
     * Convert a CategoryUiModel back to a domain Category.
     */
    fun fromUiModel(uiModel: CategoryUiModel): Category {
        return Category(
            id = uiModel.id,
            name = uiModel.name,
            channelCount = uiModel.channelCount
        )
    }
}
