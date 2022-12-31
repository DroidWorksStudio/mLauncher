package com.github.therepoclub.slider

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*

/**
 * Material Slider allows to choose height for track and thumb radius and selection between
 * [Color] or [Brush] using [SliderBrushColor]. If brush of [SliderBrushColor.brush] is
 * not null gradient
 * provided in this [Brush] is used for drawing otherwise solid color
 * [SliderBrushColor.solidColor] is used and Thumb as Icon, emoji or any desired Composable.
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
 * @param thumb thumb of the the slider
 * @param colors [MaterialSliderColors] that will be used to determine the color of the Slider parts in
 * different state. See [MaterialSliderDefaults.defaultColors],
 * [MaterialSliderDefaults.customColors] or other functions to customize.
 * @param borderStroke draws border around the track with given width in dp.
 * @param drawInactiveTrack flag to draw **InActive** track between active progress and track end.
 * @param coerceThumbInTrack when set to true track's start position is matched to thumbs left
 * on start and thumbs right at the end of the track. Use this when [trackHeight] is bigger than
 * [thumb].
 */
@Composable
fun ColorfulIconSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    trackHeight: Dp = TrackHeight,
    colors: MaterialSliderColors = MaterialSliderDefaults.defaultColors(),
    borderStroke: BorderStroke? = null,
    drawInactiveTrack: Boolean = true,
    coerceThumbInTrack: Boolean = false,
    thumb: @Composable () -> Unit
) {
    ColorfulIconSlider(
        modifier = modifier,
        value = value,
        onValueChange = { progress, _ ->
            onValueChange(progress)
        },
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        trackHeight = trackHeight,
        colors = colors,
        borderStroke = borderStroke,
        drawInactiveTrack = drawInactiveTrack,
        coerceThumbInTrack = coerceThumbInTrack,
        thumb = thumb
    )
}

/**
 * Material Slider allows to choose height for track and thumb radius and selection between
 * [Color] or [Brush] using [SliderBrushColor]. If brush of [SliderBrushColor.brush] is
 * not null gradient
 * provided in this [Brush] is used for drawing otherwise solid color
 * [SliderBrushColor.solidColor] is used and Thumb as Icon, emoji or any desired Composable.
 *
 * @param value current value of the Slider. If outside of [valueRange] provided, value will be
 * coerced to this range.
 * @param onValueChange lambda that returns value, position of **thumb** as [Offset], vertical
 * center is stored in y.
 * @param modifier modifiers for the Slider layout
 * @param enabled whether or not component is enabled and can be interacted with or not
 * @param valueRange range of values that Slider value can take. Passed [value] will be coerced to
 * this range
 * @param steps if greater than 0, specifies the amounts of discrete values, evenly distributed
 * between across the whole value range. If 0, slider will behave as a continuous slider and allow
 * to choose any value from the range specified. Must not be negative.
 * @param trackHeight height of the track that will be drawn on [Canvas]. half of [trackHeight]
 * is used as **stroke** width.
 * @param thumb thumb of the the slider
 * @param colors [MaterialSliderColors] that will be used to determine the color of the Slider parts in
 * different state. See [MaterialSliderDefaults.defaultColors],
 * [MaterialSliderDefaults.customColors] or other functions to customize.
 * @param borderStroke draws border around the track with given width in dp.
 * @param drawInactiveTrack flag to draw **InActive** track between active progress and track end.
 * @param coerceThumbInTrack when set to true track's start position is matched to thumbs left
 * on start and thumbs right at the end of the track. Use this when [trackHeight] is bigger than
 * [thumb].
 */
@Composable
fun ColorfulIconSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float, Offset) -> Unit,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    trackHeight: Dp = TrackHeight,
    colors: MaterialSliderColors = MaterialSliderDefaults.defaultColors(),
    borderStroke: BorderStroke? = null,
    drawInactiveTrack: Boolean = true,
    coerceThumbInTrack: Boolean = false,
    thumb: @Composable () -> Unit
) {

    SliderComposeLayout(
        modifier = modifier
            .minimumTouchTargetSize()
            .requiredSizeIn(
                minWidth = ThumbRadius * 2,
                minHeight = ThumbRadius * 2,
            ),
        thumb = { thumb() }
    ) { thumbSize: IntSize, constraints: Constraints ->

        require(steps >= 0) { "steps should be >= 0" }
        val onValueChangeState = rememberUpdatedState(onValueChange)
        val tickFractions = remember(steps) {
            stepsToTickFractions(steps)
        }

        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

        val width = constraints.maxWidth.toFloat()
        val thumbHalfWidthPx = (thumbSize.width / 2).toFloat()
        val thumbHeightPx = thumbSize.height.toFloat()

        // Start of the track used for measuring progress,
        // it's line + radius of cap which is half of height of track
        // to draw this on canvas starting point of line
        // should be at trackStart + trackHeightInPx / 2 while drawing
        val trackStart: Float
        // End of the track that is used for measuring progress
        val trackEnd: Float
        val strokeRadius: Float
        with(LocalDensity.current) {

            strokeRadius = trackHeight.toPx() / 2
            trackStart = thumbHalfWidthPx.coerceAtLeast(strokeRadius)
            trackEnd = width - trackStart
        }

        // Scales and interpolates from offset from dragging to user value in valueRange
        fun scaleToUserValue(offset: Float) =
            scale(trackStart, trackEnd, offset, valueRange.start, valueRange.endInclusive)

        // Scales user value using valueRange to position on x axis on screen
        fun scaleToOffset(userValue: Float) =
            scale(valueRange.start, valueRange.endInclusive, userValue, trackStart, trackEnd)

        val rawOffset = remember { mutableStateOf(scaleToOffset(value)) }

        CorrectValueSideEffect(
            ::scaleToOffset,
            valueRange,
            trackStart..trackEnd,
            rawOffset,
            value
        )

        val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
        val fraction = calculateFraction(valueRange.start, valueRange.endInclusive, coerced)

        val dragModifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change: PointerInputChange, _: Offset ->
                        if (enabled) {
                            rawOffset.value =
                                if (!isRtl) change.position.x else trackEnd - change.position.x
                            val offsetInTrack = rawOffset.value.coerceIn(trackStart, trackEnd)
                            onValueChangeState.value.invoke(
                                scaleToUserValue(offsetInTrack),
                                Offset(rawOffset.value.coerceIn(trackStart, trackEnd), strokeRadius)
                            )
                        }

                    },
                    onDragEnd = {
                        if (enabled) {
                            onValueChangeFinished?.invoke()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { position: Offset ->
                    if (enabled) {
                        rawOffset.value =
                            if (!isRtl) position.x else trackEnd - position.x
                        val offsetInTrack = rawOffset.value.coerceIn(trackStart, trackEnd)
                        onValueChangeState.value.invoke(
                            scaleToUserValue(offsetInTrack),
                            Offset(rawOffset.value.coerceIn(trackStart, trackEnd), strokeRadius)
                        )
                    }
                }
            }

        IconSliderImpl(
            enabled = enabled,
            fraction = fraction,
            trackStart = trackStart,
            trackEnd = trackEnd,
            tickFractions = tickFractions,
            colors = colors,
            trackHeight = trackHeight,
            thumbRadius = thumbHalfWidthPx,
            thumbHeight = thumbHeightPx,
            thumb = thumb,
            coerceThumbInTrack = coerceThumbInTrack,
            drawInactiveTrack = drawInactiveTrack,
            borderStroke = borderStroke,
            modifier = dragModifier
        )

    }
}

@Composable
private fun IconSliderImpl(
    enabled: Boolean,
    fraction: Float,
    trackStart: Float,
    trackEnd: Float,
    tickFractions: List<Float>,
    colors: MaterialSliderColors,
    trackHeight: Dp,
    thumbRadius: Float,
    thumbHeight: Float,
    thumb: @Composable () -> Unit,
    coerceThumbInTrack: Boolean,
    drawInactiveTrack: Boolean,
    borderStroke: BorderStroke? = null,
    modifier: Modifier
) {

    val trackStrokeWidth: Float

    var borderWidth = 0f
    val borderBrush: Brush? = borderStroke?.brush
    val thumbHeightDp: Dp

    with(LocalDensity.current) {
        trackStrokeWidth = trackHeight.toPx()
        thumbHeightDp = (2 * thumbRadius.coerceAtLeast(thumbHeight)).toDp()

        if (borderStroke != null) {
            borderWidth = borderStroke.width.toPx()
        }
    }

    Box(
        // Constraint max height of Slider to max of thumb or track or minimum touch 48.dp
        modifier.heightIn(
            max = trackHeight
                .coerceAtLeast(thumbHeightDp)
                .coerceAtLeast(TrackHeight)
        ),
        contentAlignment = Alignment.CenterStart
    ) {

        // Position that corresponds to center of this slider's thumb
        val thumbCenterPos = (trackStart + (trackEnd - trackStart) * fraction)

        Track(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxSize(),
            fraction = fraction,
            tickFractions = tickFractions,
            thumbRadius = thumbRadius,
            trackStart = trackStart,
            trackHeight = trackStrokeWidth,
            coerceThumbInTrack = coerceThumbInTrack,
            colors = colors,
            enabled = enabled,
            borderBrush = borderBrush,
            borderWidth = borderWidth,
            drawInactiveTrack = drawInactiveTrack
        )

        Box(modifier = modifier
            .offset { IntOffset((thumbCenterPos - thumbRadius).toInt(), 0) }
        ) {
            thumb()
        }
    }
}

/**
 * Draws active and if [drawInactiveTrack] is set to true inactive tracks on Canvas.
 * If inactive track is to be drawn it's drawn between start and end of canvas. Active track
 * is drawn between start and current value.
 *
 * Drawing both tracks use [SliderBrushColor] to draw a nullable [Brush] first. If it's not then
 * [SliderBrushColor.solidColor] is used to draw with solid colors provided by [MaterialSliderColors]
 */
@Composable
private fun Track(
    modifier: Modifier,
    fraction: Float,
    tickFractions: List<Float>,
    thumbRadius: Float,
    trackStart: Float,
    trackHeight: Float,
    coerceThumbInTrack: Boolean,
    colors: MaterialSliderColors,
    enabled: Boolean,
    borderBrush: Brush?,
    borderWidth: Float,
    drawInactiveTrack: Boolean,
) {

    val debug = false

    // Colors for drawing track and/or ticks
    val activeTrackColor: Brush =
        colors.trackColor(enabled = enabled, active = true).value
    val inactiveTrackColor: Brush =
        colors.trackColor(enabled = enabled, active = false).value
    val inactiveTickColor = colors.tickColor(enabled, active = false).value
    val activeTickColor = colors.tickColor(enabled, active = true).value

    // stroke radius is used for drawing length it adds this radius to both sides of the line
    val strokeRadius = trackHeight / 2

    // Start of drawing in Canvas
    // when not coerced set start of drawing line at trackStart + strokeRadius
    // to limit drawing start edge at track start end edge at track end

    // When coerced move edges of drawing by thumb radius to cover thumb edges in drawing
    // it needs to move to right as stroke radius minus thumb radius to match track start
    val drawStart =
        if (coerceThumbInTrack) trackStart - thumbRadius + strokeRadius else trackStart

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val isRtl = layoutDirection == LayoutDirection.Rtl

        val centerY = center.y

        // left side of the slider that is drawn on canvas, left tip of stroke radius on left side
        val sliderLeft = Offset(drawStart, centerY)
        // right side of the slider that is drawn on canvas, right tip of stroke radius on left side
        val sliderRight = Offset((width - drawStart).coerceAtLeast(drawStart), centerY)

        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight

        val sliderValue = Offset(
            sliderStart.x + (sliderEnd.x - sliderStart.x) * fraction,
            center.y
        )

        // InActive Track
        drawLine(
            brush = inactiveTrackColor,
            start = sliderStart,
            end = sliderEnd,
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )

        // Active Track
        drawLine(
            brush = activeTrackColor,
            start = sliderStart,
            end = if (drawInactiveTrack) sliderValue else sliderEnd,
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )

        if (debug) {
            drawLine(
                color = Color.Yellow,
                start = sliderStart,
                end = sliderEnd,
                strokeWidth = strokeRadius / 4
            )
        }

        borderBrush?.let { brush ->
            drawRoundRect(
                brush = brush,
                topLeft = Offset(sliderStart.x - strokeRadius, (height - trackHeight) / 2),
                size = Size(width = sliderEnd.x - sliderStart.x + trackHeight, trackHeight),
                cornerRadius = CornerRadius(strokeRadius, strokeRadius),
                style = Stroke(width = borderWidth)
            )
        }

        if (drawInactiveTrack) {
            tickFractions.groupBy { it > fraction }
                .forEach { (outsideFraction, list) ->
                    drawPoints(
                        points = list.map {
                            Offset(
                                androidx.compose.ui.geometry.lerp(sliderStart, sliderEnd, it).x,
                                center.y
                            )
                        },
                        pointMode = PointMode.Points,
                        brush = if (outsideFraction) inactiveTickColor
                        else activeTickColor,
                        strokeRadius.coerceAtMost(thumbRadius / 2),
                        cap = StrokeCap.Round
                    )
                }
        }
    }
}

enum class SlotsEnum {
    Slider, Thumb
}

/**
 * [SubcomposeLayout] that measure [thumb] size to set Slider's track start and track width.
 * @param thumb thumb Composable
 * @param slider Slider composable that contains **thumb** and **track** of this Slider.
 */
@Composable
private fun SliderComposeLayout(
    modifier: Modifier = Modifier,
    thumb: @Composable () -> Unit,
    slider: @Composable (IntSize, Constraints) -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints: Constraints ->

        // Subcompose(compose only a section) main content and get Placeable
        val thumbPlaceable: Placeable = subcompose(SlotsEnum.Thumb, thumb).map {
            it.measure(constraints)
        }.first()

        // Width and height of the thumb Composable
        val thumbSize = IntSize(thumbPlaceable.width, thumbPlaceable.height)

        // Whole Slider Composable
        val sliderPlaceable: Placeable = subcompose(SlotsEnum.Slider) {
            slider(thumbSize, constraints)
        }.map {
            it.measure(constraints)
        }.first()

        val sliderWidth = sliderPlaceable.width
        val sliderHeight = sliderPlaceable.height

        layout(sliderWidth, sliderHeight) {
            sliderPlaceable.placeRelative(0, 0)
        }
    }
}
