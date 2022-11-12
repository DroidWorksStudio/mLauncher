package app.olaunchercf.ui.compose

import SettingsTheme
import android.content.Context
import android.graphics.Paint.Align
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import app.olaunchercf.R
import app.olaunchercf.data.EnumOption
import app.olaunchercf.style.CORNER_RADIUS

object SettingsComposable {

    // Most basic settings background tile
    @Composable
    fun SettingsTile(content: @Composable () -> Unit) {
        Column(
            modifier = Modifier
                .padding(12.dp, 12.dp, 12.dp, 0.dp)
                .background(SettingsTheme.color.settings, SettingsTheme.shapes.settings)
                .border(
                    0.5.dp,
                    colorResource(R.color.blackInverseTrans50),
                    RoundedCornerShape(CORNER_RADIUS),
                )
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            content()
        }
    }

    @Composable
    fun SettingsArea (
        title: String,
        selected: MutableState<String>,
        fontSize: TextUnit = TextUnit.Unspecified,
        items: Array<@Composable (MutableState<Boolean>, (Boolean) -> Unit ) -> Unit>
    ) {
        SettingsTile {
            SettingsTitle(text = title, fontSize = fontSize)
            items.forEachIndexed { i, item ->
                item(mutableStateOf("$title-$i" == selected.value)) { b ->
                    val number = if (b) i else -1
                    selected.value = "$title-$number"
                }
            }

        }
    }

    @Composable
    fun SettingsTopView(title: String, onClick: () -> Unit = {}, fontSize: TextUnit = TextUnit.Unspecified, iconSize: Dp = 16.dp, content: @Composable () -> Unit) {
        SettingsTile {
            Box(modifier = Modifier.fillMaxWidth()) {
                SettingsTitle(
                    text = title,
                    fontSize = fontSize,
                    modifier = Modifier.align(CenterStart)
                )
                Image(
                    painterResource(R.drawable.ic_outline_info_24),
                    contentDescription = "",
                    modifier = Modifier.size(iconSize).align(TopEnd).clickable { onClick() },
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
        SettingsRow(
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
        currentSelection: MutableState<T>,
        values: Array<T>,
        open: MutableState<Boolean>,
        onChange: (Boolean) -> Unit,
        fontSize: TextUnit = TextUnit.Unspecified,
        onSelect: (T) -> Unit,
    ) {
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
                SettingsSelector(values, fontSize = fontSize) { i ->
                    onChange(false)
                    currentSelection.value = i
                    onSelect(i)
                }
            }
        } else {
            SettingsRow(
                title = title,
                onClick = { onChange(true) },
                fontSize = fontSize,
                buttonText = currentSelection.value.string()
            )
        }
    }

    @Composable
    fun SettingsNumberItem(
        title: String,
        currentSelection: MutableState<Int>,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        open: MutableState<Boolean>,
        onChange: (Boolean) -> Unit,
        onValueChange: (Int) -> Unit = {},
        fontSize: TextUnit = TextUnit.Unspecified,
        onSelect: (Int) -> Unit
    ) {
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
            SettingsRow(
                title = title,
                onClick = { onChange(true) },
                fontSize = fontSize,
                buttonText = currentSelection.value.toString()
            )
        }
    }

    @Composable
    fun SettingsAppSelector(
        title: String,
        currentSelection: MutableState<String>,
        active: Boolean,
        fontSize: TextUnit = TextUnit.Unspecified,
        onClick: () -> Unit,
    ) {
        SettingsRow(
            title = title,
            onClick = onClick,
            buttonText = currentSelection.value,
            active = active,
            fontSize = fontSize,
            disabledText = stringResource(R.string.disabled)
        )
    }

    @Composable
    private fun SettingsRow(
        title: String,
        onClick: () -> Unit,
        buttonText: String,
        active: Boolean = true,
        disabledText: String = buttonText,
        fontSize: TextUnit = TextUnit.Unspecified,
    ) {
        ConstraintLayout(modifier = Modifier.fillMaxWidth()) {

            val (text, button) = createRefs()

            Box(
                modifier = Modifier
                    .constrainAs(text) {
                        start.linkTo(parent.start)
                        end.linkTo(button.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.fillToConstraints
                    },
            ) {
                Text(
                    title,
                    style = SettingsTheme.typography.item,
                    fontSize = fontSize,
                    modifier = Modifier.align(CenterStart)
                )
            }

            TextButton(
                onClick = onClick,
                modifier = Modifier.constrainAs(button) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            ) {
                Text(
                    text = if (active) buttonText else disabledText,
                    style = if (active) SettingsTheme.typography.button else SettingsTheme.typography.buttonDisabled,
                    fontSize = fontSize,
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
                    .align(Alignment.CenterEnd),
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
    private fun SettingsNumberSelector(
        number: MutableState<Int>,
        min: Int,
        max: Int,
        fontSize: TextUnit = TextUnit.Unspecified,
        onValueChange: (Int) -> Unit = {},
        onCommit: (Int) -> Unit
    ) {
        ConstraintLayout(
            modifier = Modifier
                .background(SettingsTheme.color.selector, SettingsTheme.shapes.settings)
                .fillMaxWidth()
        ) {
            val (plus, minus, text, button) = createRefs()
            TextButton(
                onClick = {
                    if (number.value > min) {
                        number.value -= 1
                        onValueChange(number.value)
                    }
                },
                modifier = Modifier.constrainAs(minus) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(text.end)
                    end.linkTo(button.start)
                },
            ) {
                Text("-", style = SettingsTheme.typography.button, fontSize = fontSize)
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
                    if (number.value < max) {
                        number.value += 1
                        onValueChange(number.value)
                    }
                },
                modifier = Modifier.constrainAs(plus) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(text.start)
                },
            ) {
                Text("+", style = SettingsTheme.typography.button, fontSize = fontSize)
            }
            TextButton(
                onClick = { onCommit(number.value) },
                modifier = Modifier.constrainAs(button) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(minus.end)
                    end.linkTo(parent.end)
                },
            ) {
                Text(stringResource(R.string.save), style = SettingsTheme.typography.button, fontSize = fontSize)
            }
        }
    }

    @Composable
    fun SimpleTextButton(title: String, fontSize: TextUnit = TextUnit.Unspecified, onClick: () -> Unit) {
        TextButton(
            onClick = onClick,
        ){
            Text(
                title,
                style = SettingsTheme.typography.item,
                fontSize = fontSize,
            )
        }
    }
}