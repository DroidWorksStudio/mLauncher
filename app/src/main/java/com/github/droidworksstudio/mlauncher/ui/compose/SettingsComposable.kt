package com.github.droidworksstudio.mlauncher.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.droidworksstudio.mlauncher.style.SettingsTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SettingsComposable {

    @Composable
    fun PageHeader(
        @DrawableRes iconRes: Int,
        title: String,
        iconSize: Dp = 24.dp, // Default size for the icon
        fontSize: TextUnit = 24.sp, // Default font size for the title
        onClick: () -> Unit = {},
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image Icon
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                colorFilter = ColorFilter.tint(SettingsTheme.color.image),
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .size(iconSize)
            )

            Spacer(modifier = Modifier.weight(1f)) // Pushes the text to center

            // Title Text
            Text(
                text = title,
                style = SettingsTheme.typography.title,
                fontSize = fontSize
            )

            Spacer(modifier = Modifier.weight(1f)) // Balances spacing on the right
        }
    }

    @Composable
    fun TopMainHeader(
        @DrawableRes iconRes: Int,
        title: String,
        iconSize: Dp = 96.dp, // Default size for the icon
        fontSize: TextUnit = 24.sp, // Default font size for the title
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp), // Optional horizontal padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Icon
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier
                    .size(iconSize)
                    .padding(bottom = 16.dp) // Bottom margin like in XML
            )

            // Title Text
            Text(
                text = title,
                style = SettingsTheme.typography.title,
                fontSize = fontSize,
                modifier = Modifier
                    .padding(bottom = 24.dp)
            )
        }
    }

    @Composable
    fun SettingsHomeItem(
        title: String,
        description: String? = null,
        imageVector: ImageVector,
        onClick: () -> Unit = {},
        onMultiClick: (Int) -> Unit = {}, // now takes tap count
        enableMultiClick: Boolean = false,
        titleFontSize: TextUnit = TextUnit.Unspecified,
        descriptionFontSize: TextUnit = TextUnit.Unspecified,
        fontColor: Color = SettingsTheme.typography.title.color,
        iconSize: Dp = 18.dp,
        multiClickCount: Int = 5,
        multiClickInterval: Long = 2000L
    ) {
        var tapCount by remember { mutableIntStateOf(0) }
        var lastTapTime by remember { mutableLongStateOf(0L) }

        val scope = rememberCoroutineScope()
        var clickJob by remember { mutableStateOf<Job?>(null) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (!enableMultiClick) {
                        onClick()
                        return@clickable
                    }

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTapTime > multiClickInterval) {
                        tapCount = 0
                    }

                    tapCount++
                    lastTapTime = currentTime
                    clickJob?.cancel()

                    if (tapCount >= multiClickCount) {
                        tapCount = 0
                        onMultiClick(multiClickCount)
                    } else {
                        onMultiClick(tapCount)
                        clickJob = scope.launch {
                            delay(multiClickInterval)
                            if (tapCount == 1) {
                                onClick()
                            }
                            tapCount = 0
                        }
                    }
                }

                .padding(vertical = 16.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                imageVector = imageVector,
                contentDescription = title,
                colorFilter = ColorFilter.tint(SettingsTheme.color.image),
                modifier = Modifier.size(iconSize)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = SettingsTheme.typography.title,
                    fontSize = titleFontSize,
                    color = fontColor
                )
                description?.let {
                    Text(
                        text = it,
                        style = SettingsTheme.typography.title,
                        fontSize = descriptionFontSize,
                        color = fontColor
                    )
                }
            }
        }
    }

    @Composable
    fun SettingsTitle(
        text: String,
        modifier: Modifier = Modifier,
        fontSize: TextUnit = TextUnit.Unspecified
    ) {
        // Text
        Text(
            text = text,
            style = SettingsTheme.typography.header,
            fontSize = fontSize,
            modifier = modifier
                .padding(start = 16.dp)
                .padding(top = 16.dp)
        )
    }


    @Composable
    fun SettingsSwitch(
        text: String,
        fontSize: TextUnit = TextUnit.Unspecified,
        defaultState: Boolean = false,
        onCheckedChange: (Boolean) -> Unit
    ) {
        var isChecked by remember { mutableStateOf(defaultState) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = SettingsTheme.typography.title,
                fontSize = fontSize,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = isChecked,
                onCheckedChange = {
                    isChecked = it
                    onCheckedChange(it) // Notify parent
                },
                modifier = Modifier.scale(0.7f), // Mimic XML scaling
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.Green,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.LightGray
                )
            )
        }
    }

    @Composable
    fun SettingsSelect(
        title: String,
        option: String,
        fontSize: TextUnit = 24.sp,
        fontColor: Color = SettingsTheme.typography.title.color,
        onClick: () -> Unit = {},
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = SettingsTheme.typography.title,
                fontSize = fontSize,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = option,
                style = SettingsTheme.typography.title,
                fontSize = fontSize,
                color = fontColor,
                modifier = Modifier
                    .clickable {
                        onClick()
                    }
            )
        }
    }
}