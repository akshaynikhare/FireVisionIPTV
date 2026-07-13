package com.cadnative.firevisioniptv.presentation.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.cadnative.firevisioniptv.presentation.ui.theme.Dimens
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryColor
import com.cadnative.firevisioniptv.presentation.ui.theme.categoryIcon

/** The vertical "now" line placed at [offset] dp from the lane's left edge. */
@Composable
internal fun GuideNowLine(
    offset: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .offset(x = offset)
            .fillMaxHeight()
            .width(Dimens.GuideNowLineWidth)
            .background(MaterialTheme.colorScheme.primary)
    )
}

/** Channel identity in the sticky left column: number, logo (or genre icon), name. */
@Composable
internal fun GuideChannelHeader(
    name: String,
    number: Int,
    logoUrl: String?,
    category: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Space4, vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space3)
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .size(Dimens.GuideChannelLogoSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(Dimens.GuideChannelLogoSize).clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = null,
                    tint = categoryColor(category),
                    modifier = Modifier.size(Dimens.IconMedium)
                )
            }
        }
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
