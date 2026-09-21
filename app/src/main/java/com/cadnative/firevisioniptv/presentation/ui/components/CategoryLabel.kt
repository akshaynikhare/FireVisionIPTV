package com.cadnative.firevisioniptv.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cadnative.firevisioniptv.R
import com.cadnative.firevisioniptv.domain.model.CategorySentinels

/**
 * The visible name for a category.
 *
 * Every other category name is server data and stays verbatim. Only
 * [CategorySentinels.OTHER] is ours — it is the grouping key we substitute for a
 * channel with no category, so it must stay untranslated everywhere it is
 * compared, keyed or navigated with, and be translated only here, at the point
 * it becomes text on screen.
 */
@Composable
fun categoryLabel(category: String): String =
    if (category == CategorySentinels.OTHER) stringResource(R.string.category_other) else category
