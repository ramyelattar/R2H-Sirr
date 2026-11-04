package com.r2h.sirr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

/**
 * Shared, accessible brand identity for R2H-Play.
 *
 * Media builds supply drawable-nodpi/r2h_play_cosmic_logo.png, derived from
 * the owner's existing file, rather than a second unrelated icon.
 * Resolving the optional resource allows source-only GitHub branches to
 * compile even before the owner-supplied binary is packaged. The preview
 * build MUST verify that the drawable exists in its assembled APK.
 */
@Composable
internal fun R2HBrandLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val context = LocalContext.current
    val logoId = remember(context, compact) {
        val preferred = if (compact) "r2h_play_cosmic_header" else "r2h_play_cosmic_logo"
        val specific = context.resources.getIdentifier(
            preferred, "drawable", context.packageName
        )
        if (specific != 0) specific else context.resources.getIdentifier(
            "r2h_play_cosmic_logo", "drawable", context.packageName
        )
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (logoId != 0) {
            Image(
                painter = painterResource(logoId),
                contentDescription = "R2H Play",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                "R2H Play",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFE2D8E9)
            )
        }
    }
}
