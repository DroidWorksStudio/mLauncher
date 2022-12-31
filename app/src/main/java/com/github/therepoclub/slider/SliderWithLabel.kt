package com.github.therepoclub.slider

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

enum class LabelPosition {
    Top, Bottom
}

/**
 * Slider with [ColorfulSlider] and optional label to display info above or below thumb.
 **
 * You can allow the user to choose only between predefined set of values by specifying the amount
 * of steps between min and max values:
 *
 * Material Slider allows to choose height for track and thumb radius and selection between
 * [Color] or [Brush] using [SliderBrushColor]. If brush of [SliderBrushColor.brush] is
 * not null gradient
 * provided in this [Brush] is used for drawing otherwise solid color
 * [SliderBrushColor.solidColor] is used.
 *
 * @param value current value of the Slider. If outside of [valueRange] provided, value will be
 * coerced to this range.
 * @param onValueChange lambda that returns value.
 * @param modifier modifiers for the Slider layout.
 * @param enabled whether or not component is enabled and can be interacted with or not
 * @param valueRange range of values that Slider value can take. Passed [value] will be coerced to
 * this range
 * @param steps if greater than 0, specifies the amounts of discrete values, evenly distributed
 * between across the whole value range. If 0, slider will behave as a continuous slider and allow
 * to choose any value from the range specified. Must not be negative.
 * @param trackHeight height of the track that will be drawn on [Canvas]. half of [trackHeight]
 * is used as **stroke** width.
 * @param thumbRadius radius of thumb of the the slider
 * @param colors [MaterialSliderColors] that will be used to determine the color of the Slider parts in
 * different state. See [MaterialSliderDefaults.defaultColors],
 * [MaterialSliderDefaults.customColors] or other functions to customize.
 * @param borderStroke draws border around the track with given width in dp.
 * @param drawInactiveTrack flag to draw **InActive** track between active progress and track end.
 * @param coerceThumbInTrack when set to true track's start position is matched to thumbs left
 * on start and thumbs right at the end of the track. Use this when [trackHeight] is bigger than
 * [thumbRadius].
 * @param labelPosition position of label. Label can be on top or at the bottom of the Slider thumb.
 * @param yOffset position of label relative to slider. Positive offset moves label downwards.
 * on y axis.
 * @param label is a Composable that cen be text or image above or below thumb's center.
 */
@Composable
fun SliderWithLabel(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    trackHeight: Dp = TrackHeight,
    thumbRadius: Dp = ThumbRadius,
    colors: MaterialSliderColors = MaterialSliderDefaults.defaultColors(),
    borderStroke: BorderStroke? = null,
    drawInactiveTrack: Boolean = true,
    coerceThumbInTrack: Boolean = false,
    labelPosition: LabelPosition = LabelPosition.Top,
    yOffset: Dp = 0.dp,
    label: @Composable () -> Unit = {}
) {
    BoxWithConstraints {

        val maxWidth = constraints.maxWidth.toFloat()
        Column {

            val yOffsetInt = with(LocalDensity.current) {
                yOffset.toPx().toInt()
            }

            var labelOffset by remember {
                mutableStateOf(
                    Offset(
                        x = scale(
                            valueRange.start, valueRange.endInclusive, value, 0f, maxWidth
                        ),
                        y = 0f
                    )
                )
            }
            var labelWidth by remember { mutableStateOf(0) }

            if (labelPosition == LabelPosition.Top) {
                Box(Modifier
                    .offset {
                        IntOffset(labelOffset.x.toInt() - labelWidth / 2, yOffsetInt)
                    }
                    .onSizeChanged {
                        labelWidth = it.width
                    }
                ) {
                    label()
                }
                ColorfulSlider(
                    modifier = modifier,
                    value = value,
                    onValueChange = { value, offset ->
                        labelOffset = offset
                        onValueChange(value)
                    },
                    enabled = enabled,
                    valueRange = valueRange,
                    steps = steps,
                    onValueChangeFinished = onValueChangeFinished,
                    trackHeight = trackHeight,
                    thumbRadius = thumbRadius,
                    borderStroke = borderStroke,
                    drawInactiveTrack = drawInactiveTrack,
                    coerceThumbInTrack = coerceThumbInTrack,
                    colors = colors
                )
            } else {
                ColorfulSlider(
                    modifier = modifier,
                    value = value,
                    onValueChange = { value, offset ->
                        labelOffset = offset
                        onValueChange(value)
                    },
                    enabled = enabled,
                    valueRange = valueRange,
                    steps = steps,
                    onValueChangeFinished = onValueChangeFinished,
                    trackHeight = trackHeight,
                    thumbRadius = thumbRadius,
                    borderStroke = borderStroke,
                    drawInactiveTrack = drawInactiveTrack,
                    coerceThumbInTrack = coerceThumbInTrack,
                    colors = colors
                )

                Box(Modifier
                    .offset {
                        IntOffset(labelOffset.x.toInt() - labelWidth / 2, yOffsetInt)
                    }
                    .onSizeChanged {
                        labelWidth = it.width
                    }
                ) {
                    label()
                }
            }
        }
    }
}