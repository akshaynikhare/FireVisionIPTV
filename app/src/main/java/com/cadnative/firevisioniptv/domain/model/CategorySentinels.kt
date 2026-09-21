package com.cadnative.firevisioniptv.domain.model

/**
 * Category values that are identity, not copy.
 *
 * [OTHER] stands in for a channel with no category and is used as a map key for
 * grouping and weighting. It reaches the screen as a section heading too, which
 * makes it look translatable — it is not. Localise it at the render site and
 * leave every grouping comparison on this constant, or the groups stop matching
 * as soon as the device language changes.
 */
object CategorySentinels {
    const val OTHER = "Other"
}
