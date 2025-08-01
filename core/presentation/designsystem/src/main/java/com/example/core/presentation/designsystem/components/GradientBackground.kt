package com.example.core.presentation.designsystem.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.presentation.designsystem.RuniquePLTheme

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    hasToolbar: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = density.run {
        configuration.screenWidthDp.dp.roundToPx()
    }

    val smallerDimension = minOf(
        configuration.screenWidthDp.dp,
        configuration.screenHeightDp.dp
    )
    val smallerDimensionPx = density.run { smallerDimension.roundToPx() }


    val isNewerThanAndroid12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val blurColor =
        if(isNewerThanAndroid12)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3F)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = modifier
            .fillMaxSize()
            .then(
                if (isNewerThanAndroid12) {
                    Modifier.blur(
                        smallerDimension / 3
                    )
                } else Modifier
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        blurColor,
                        MaterialTheme.colorScheme.background
                    ),
                    center = Offset(
                        x = screenWidthPx / 2f,
                        y = -100f
                    ),
                    radius = smallerDimensionPx / 2f
                )
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hasToolbar) {
                        Modifier
                    } else {
                        Modifier.systemBarsPadding()
                    }
                )
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun GradientBackgroundPreview() {
    RuniquePLTheme {
        GradientBackground(modifier = Modifier.fillMaxSize()) {

        }
    }
}

