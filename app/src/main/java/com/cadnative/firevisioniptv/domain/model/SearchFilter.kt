package com.cadnative.firevisioniptv.domain.model

/**
 * Sealed class representing different types of search filters.
 * 
 * Filters can be applied to channel search results to narrow down
 * the results based on various criteria. Multiple filters can be
 * combined using the Combined filter type.
 */
sealed class SearchFilter {
    /**
     * Filter channels by category.
     */
    data class ByCategory(val category: String) : SearchFilter()
    
    /**
     * Filter channels by language.
     */
    data class ByLanguage(val language: String) : SearchFilter()
    
    /**
     * Filter channels by country.
     */
    data class ByCountry(val country: String) : SearchFilter()
    
    /**
     * Combine multiple filters with AND logic.
     * All filters in the list must match for a channel to be included.
     */
    data class Combined(val filters: List<SearchFilter>) : SearchFilter()
}
