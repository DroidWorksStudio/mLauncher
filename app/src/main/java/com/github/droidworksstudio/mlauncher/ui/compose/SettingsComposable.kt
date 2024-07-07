package com.github.droidworksstudio.mlauncher.ui.compose

import android.graphics.Typeface
import androidx.compose.foundation.*
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Alignment.Companion.Start
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.Constants
import com.github.droidworksstudio.mlauncher.data.EnumOption
import com.github.droidworksstudio.mlauncher.style.BORDER_SIZE
import com.github.droidworksstudio.mlauncher.style.CORNER_RADIUS
import com.github.droidworksstudio.mlauncher.style.SETTINGS_PADDING
import com.smarttoolfactory.slider.*
import java.math.BigDecimal
import java.math.RoundingMode

object SettingsComposable {

    // Most basic settings background tile
    @Composable
    fun SettingsTile(content: @Composable () -> Unit) {
        Column(
            modifier = Modifier
                .padding(6.dp, 6.dp, 6.dp, 0.dp)
                .background(SettingsTheme.color.settings, SettingsTheme.shapes.settings)
                .border(
                    BORDER_SIZE,
                    SettingsTheme.color.border,
                    RoundedCornerShape(CORNER_RADIUS),
                )
                .padding(SETTINGS_PADDING)
                .fillMaxWidth()
        ) {
            content()
        }
    }

    @Composable
    fun SettingsArea(
        title: String,
        selected: MutableState<String>,
        fontSize: TextUnit = TextUnit.Unspecified,
        items: Array<@Composable (MutableState<Boolean>, (Boolean) -> Unit) -> Unit>
    ) {
        var key by remember { mutableIntStateOf(0) } // Add a key to force recomposition

        val itemsChanged = remember { mutableStateOf(false) } // Track changes in items

        SettingsTile {
            SettingsTitle(text = title, fontSize = fontSize)
            items.forEachIndexed { i, item ->
                item(mutableStateOf("$title-$i" == selected.value)) { b ->
                    val number = if (b) i else -1
                    selected.value = "$title-$number"
                    itemsChanged.value = true // Mark items as changed
                }
            }
        }

        // Update key whenever items change
        LaunchedEffect(itemsChanged.value) {
            if (itemsChanged.value) {
                key++
                itemsChanged.value = false
            }
        }
    }

    @Composable
    fun SettingsTopView(title: String, onClick: () -> Unit = {}, fontSize: TextUnit = TextUnit.Unspecified, iconSize: Dp = 16.dp, content: @Composable () -> Unit) {
        SettingsTile {
            Box(modifier = Modifier
                .fillMaxWidth()) {
                SettingsTitle(
                    text = title,
                    fontSize = fontSize,
                    modifier = Modifier
                        .align(CenterStart)
                )
                Image(
                    painterResource(R.drawable.ic_info),
                    contentDescription = "",
                    modifier = Modifier
                        .size(iconSize)
                        .align(TopEnd)
                        .clickable { onClick() },
                )
            }
            content()
        }
    }

    @Composable
    fun SettingsTitle(text: String, modifier: Modifier = Modifier, fontSize: TextUnit = TextUnit.Unspecified) {
        Text(
            text = text,
            style = SettingsTheme.typography.title,
            fontSize = fontSize,
            modifier = modifier
                .padding(0.dp, 0.dp, 0.dp, 12.dp)
        )
    }

    @Composable
    fun SettingsToggle(
        title: String,
        state: MutableState<Boolean>,
        onChange: (Boolean) -> Unit,
        fontSize: TextUnit = TextUnit.Unspecified,
        onToggle: () -> Unit
    ) {
        val buttonText = if (state.value) stringResource(R.string.on) else stringResource(R.string.off)
        SettingsItem(
            title = title,
            onClick = {
                onChange(false)
                state.value = !state.value
                onToggle()
            },
            fontSize = fontSize,
            buttonText = buttonText
        )
    }

    @Composable
    fun <T: EnumOption> SettingsItem(
        title: String,
        currentSelection: MutableState<EnumOption>,
        currentSelectionName: String? = null,
        values: Array<T>,
        open: MutableState<Boolean>,
        active: Boolean = true,
        onChange: (Boolean) -> Unit,
        fontSize: TextUnit = TextUnit.Unspecified,
        onSelect: (T) -> Unit,
    ) {

        Column {
            Text(
                title,
                style = SettingsTheme.typography.item,
                fontSize = fontSize,
                modifier = Modifier
                    .align(Start)
            )

            if (open.value) {
                Box(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures {
                                onChange(false)
                            }
                        }
                        .onFocusEvent {
                            if (it.isFocused) {
                                onChange(false)
                            }
                        }
                ) {
                    SettingsSelector(values, fontSize) { i ->
                        onChange(false)
                        currentSelection.value = i
                        onSelect(i)
                    }
                }
            } else {
                Box (
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    SettingsButton(
                        // title = title,
                        onClick = { onChange(true) },
                        active = active,
                        modifier = Modifier
                            .align(CenterEnd),
                        fontSize = fontSize,
                        buttonText = currentSelectionName ?: currentSelection.value.string()
                    )
                }
            }
        }

    }

    @Composable
    fun <T: EnumOption> SettingsItemFont(
        title: String,
        currentSelection: MutableState<EnumOption>,
        currentSelectionName: String? = null,
        values: Array<T>,
        open: MutableState<Boolean>,
        active: Boolean = true,
        onChange: (Boolean) -> Unit,
        typefaces: Map<Constants.Fonts, Typeface?>,
        fontSize: TextUnit = TextUnit.Unspecified,
        onSelect: (T) -> Unit,
    ) {

        Column {
            Text(
                title,
                style = SettingsTheme.typography.item,
                fontSize = fontSize,
                modifier = Modifier
                    .align(Start)
            )

            if (open.value) {
                Box(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures {
                                onChange(false)
                            }
                        }
                        .onFocusEvent {
                            if (it.isFocused) {
                                onChange(false)
                            }
                        }
                ) {
                    SettingsSelector(values, typefaces, fontSize) { i ->
                        onChange(false)
                        currentSelection.value = i
                        onSelect(i)
                    }
                }
            } else {
                Box (
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    SettingsButton(
                        // title = title,
                        onClick = { onChange(true) },
                        active = active,
                        modifier = Modifier
                            .align(CenterEnd),
                        fontSize = fontSize,
                        buttonText = currentSelectionName ?: currentSelection.value.string()
                    )
                }
            }
        }
    }

    @Composable
    fun SettingsGestureItem(
        title: String,
        open: MutableState<Boolean>,
        onChange: (Boolean) -> Unit,
        currentAction: Constants.Action,
        onSelect: (Constants.Action) -> Unit,
        fontSize: TextUnit = TextUnit.Unspecified,
        appLabel: String,
    ) {
        SettingsItem(
            open = open,
            onChange = onChange,
            title = title,
            currentSelection = remember { mutableStateOf(currentAction) },
            currentSelectionName = if (currentAction == Constants.Action.OpenApp) "Open $appLabel" else currentAction.string(),
            values = Constants.Action.values(),
            fontSize = fontSize,
            active = currentAction != Constants.Action.Disabled,
            onSelect = onSelect,
        )
    }

    @Composable
    fun SettingsNumberItem(
        title: String,
        currentSelection: MutableState<Float>,
        min: Float = Float.MIN_VALUE,
        max: Float = Float.MAX_VALUE,
        open: MutableState<Boolean>,
        onChange: (Boolean) -> Unit,
        onValueChange: (Float) -> Unit = {},
        fontSize: TextUnit = TextUnit.Unspecified,
        onSelect: (Float) -> Unit
    ) {
        Column {
            Text(
                title,
                style = SettingsTheme.typography.item,
                fontSize = fontSize,
                modifier = Modifier
                    .align(Start)
            )

            if (open.value) {
                SettingsNumberSelector(
                    number = currentSelection,
                    min = min,
                    max = max,
                    fontSize = fontSize,
                    onValueChange = onValueChange,
                ) { i ->
                    onChange(false)
                    currentSelection.value = i
                    onSelect(i)
                }
            } else {
                Box (
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    SettingsButton(
                        onClick = { onChange(true) },
                        modifier = Modifier
                            .align(CenterEnd),
                        fontSize = fontSize,
                        buttonText = currentSelection.value.toString()
                    )
                }
            }
        }
    }

    @Composable
    fun SettingsSliderItem(
        title: String,
        currentSelection: MutableState<Int>,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        open: MutableState<Boolean>,
        onChange: (Boolean) -> Unit,
        fontSize: TextUnit = TextUnit.Unspecified,
        onSelect: (Int) -> Unit
    ) {
        Column {
            Text(
                title,
                style = SettingsTheme.typography.item,
                fontSize = fontSize,
                modifier = Modifier
                    .align(Start)
            )

            if (open.value) {
                SettingsSliderSelector(
                    number = currentSelection,
                    min = min,
                    max = max,
                    fontSize = fontSize,
                ) { i ->
                    onChange(false)
                    currentSelection.value = i
                    onSelect(i)
                }
            } else {
                Box (
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    SettingsButton(
                        onClick = { onChange(true) },
                        modifier = Modifier
                            .align(CenterEnd),
                        fontSize = fontSize,
                        buttonText = currentSelection.value.toString()
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingsItem(
        title: String,
        onClick: () -> Unit,
        buttonText: String,
        active: Boolean = true,
        disabledText: String = buttonText,
        fontSize: TextUnit = TextUnit.Unspecified,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                title,
                style = SettingsTheme.typography.item,
                fontSize = fontSize,
                modifier = Modifier
                    .align(Start)
            )

            SettingsButton(
                buttonText = buttonText,
                disabledText = disabledText,
                active = active,
                onClick = onClick,
                fontSize = fontSize,
                modifier = Modifier
                    .align(End)
            )
        }
    }

    @Composable
    fun SettingsButton(
        modifier: Modifier = Modifier,
        buttonText: String,
        disabledText: String = buttonText,
        active: Boolean = true,
        onClick: () -> Unit = { },
        fontSize: TextUnit = TextUnit.Unspecified,
    ){
        TextButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            Text(
                text = if (active) buttonText else disabledText,
                fontSize = fontSize,
                style = if (active) SettingsTheme.typography.button else SettingsTheme.typography.buttonDisabled,
            )
        }
    }

    @Composable
    fun SettingsThreeButtonRow(
        firstButtonText: String,
        secondButtonText: String,
        thirdButtonText: String,
        firstButtonAction: () -> Unit,
        secondButtonAction: () -> Unit,
        thirdButtonAction: () -> Unit,
        fontSize: TextUnit = TextUnit.Unspecified,
    ) {
        Row {
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = firstButtonAction,
            ) {
                Text(
                    firstButtonText,
                    fontSize = fontSize,
                    style = SettingsTheme.typography.button,
                )
            }

            TextButton(
                onClick = secondButtonAction,
            ) {
                Text(
                    secondButtonText,
                    fontSize = fontSize,
                    style = SettingsTheme.typography.button,
                )
            }

            TextButton(
                onClick = thirdButtonAction,
            ) {
                Text(
                    thirdButtonText,
                    fontSize = fontSize,
                    style = SettingsTheme.typography.button,
                )
            }
        }
    }

    @Composable
    private fun <T: EnumOption> SettingsSelector(options: Array<T>, fontSize: TextUnit = TextUnit.Unspecified, onSelect: (T) -> Unit) {
        Box(
            modifier = Modifier
                .background(SettingsTheme.color.selector, SettingsTheme.shapes.settings)
                .fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier
                    .align(CenterEnd),
                horizontalArrangement = Arrangement.SpaceEvenly
            )
            {
                for (opt in options) {
                    item {
                        TextButton(
                            onClick = { onSelect(opt) },
                        ) {
                            Text(
                                text = opt.string(),
                                fontSize = fontSize,
                                style = SettingsTheme.typography.button
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun <T: EnumOption> SettingsSelector(options: Array<T>, typefaces: Map<Constants.Fonts, Typeface?>, fontSize: TextUnit = TextUnit.Unspecified, onSelect: (T) -> Unit) {
        Box(
            modifier = Modifier
                .background(SettingsTheme.color.selector, SettingsTheme.shapes.settings)
                .fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier
                    .align(CenterEnd),
                horizontalArrangement = Arrangement.SpaceEvenly
            )
            {
                for (opt in options) {
                    val fontFamily = typefaces[opt as Constants.Fonts]?.let { FontFamily(it) } ?: FontFamily.Default

                    item {
                        TextButton(
                            onClick = { onSelect(opt) },
                        ) {
                            Text(
                                text = opt.string(),
                                fontSize = fontSize,
                                style = SettingsTheme.typography.button.copy(
                                    fontFamily = fontFamily
                                )
                            )
                        }
                    }
                }

            }
        }
    }

    @Composable
    private fun SettingsNumberSelector(
        number: MutableState<Float>,
        min: Float,
        max: Float,
        fontSize: TextUnit = TextUnit.Unspecified,
        onValueChange: (Float) -> Unit = {},
        onCommit: (Float) -> Unit
    ) {
        ConstraintLayout(
            modifier = Modifier
                .background(SettingsTheme.color.selector, SettingsTheme.shapes.settings)
                .fillMaxWidth()
        ) {
            val (plus, minus, text, button) = createRefs()
            TextButton(
                onClick = {
                    val newValue = (BigDecimal(number.value.toDouble()) + BigDecimal("0.1")).toFloat()
                    if (newValue <= max) {
                        number.value = newValue.roundToTwoDecimalPlaces()
                        onValueChange(number.value)
                    }
                },
                modifier = Modifier
                    .constrainAs(minus) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(text.end)
                        end.linkTo(button.start)
                    },
            ) {
                Text(
                    "+",
                    style = SettingsTheme.typography.button,
                    fontSize = fontSize
                )
            }
            Text(
                text = number.value.toString(),
                fontSize = fontSize,
                modifier = Modifier
                    .fillMaxHeight()
                    .constrainAs(text) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(plus.end)
                        end.linkTo(minus.start)
                    },
                style = SettingsTheme.typography.item,
            )
            TextButton(
                onClick = {
                    val newValue = (BigDecimal(number.value.toDouble()) - BigDecimal("0.1")).toFloat()
                    if (newValue >= min) {
                        number.value = newValue.roundToTwoDecimalPlaces()
                        onValueChange(number.value)
                    }
                },
                modifier = Modifier
                    .constrainAs(plus) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(text.start)
                    },
            ) {
                Text(
                    "-",
                    style = SettingsTheme.typography.button,
                    fontSize = fontSize
                )
            }
            TextButton(
                onClick = { onCommit(number.value) },
                modifier = Modifier
                    .constrainAs(button) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(minus.end)
                        end.linkTo(parent.end)
                    },
            ) {
                Text(
                    stringResource(R.string.save),
                    style = SettingsTheme.typography.button,
                    fontSize = fontSize
                )
            }
        }
    }

    private fun Float.roundToTwoDecimalPlaces(): Float {
        return BigDecimal(this.toDouble()).setScale(2, RoundingMode.HALF_EVEN).toFloat()
    }

    @Composable
    private fun SettingsSliderSelector(
        number: MutableState<Int>,
        min: Int,
        max: Int,
        fontSize: TextUnit = TextUnit.Unspecified,
        onCommit: (Int) -> Unit
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val (text, button) = createRefs()
            var labelProgress by remember { mutableFloatStateOf(number.value.toFloat()) }
            Text(
                labelProgress.toInt().toString(),
                style = SettingsTheme.typography.button,
                fontSize = fontSize
            )
            ColorfulSlider(
                value = labelProgress,
                thumbRadius = 5.dp,
                trackHeight = 5.dp,
                onValueChange = { it -> labelProgress = it },
                valueRange = min.toFloat()..max.toFloat(),
                colors = MaterialSliderDefaults.materialColors(
                    inactiveTrackColor = SliderBrushColor(color = Color.Transparent),
                ),
                modifier = Modifier
                    .padding(end = 62.dp)
            )
            TextButton(
                onClick = { onCommit(labelProgress.toInt()) },
                modifier = Modifier
                    .constrainAs(button) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(text.end)
                        end.linkTo(parent.end)
                    },
            ) {
                Text(
                    stringResource(R.string.save),
                    style = SettingsTheme.typography.button,
                    fontSize = fontSize
                )
            }
        }
    }

    @Composable
    fun SettingsTextButton(title: String, fontSize: TextUnit = TextUnit.Unspecified, onClick: () -> Unit) {
        TextButton(
            onClick = onClick,
        ){
            Text(
                title,
                style = SettingsTheme.typography.item,
                fontSize = fontSize
            )
        }
    }
}