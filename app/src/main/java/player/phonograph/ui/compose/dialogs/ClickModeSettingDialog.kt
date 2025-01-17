/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.dialogs

import player.phonograph.R
import player.phonograph.actions.click.mode.SongClickMode.FLAG_MASK_GOTO_POSITION_FIRST
import player.phonograph.actions.click.mode.SongClickMode.FLAG_MASK_PLAY_QUEUE_IF_EMPTY
import player.phonograph.actions.click.mode.SongClickMode.baseModes
import player.phonograph.actions.click.mode.SongClickMode.modeName
import player.phonograph.settings.Setting
import player.phonograph.util.Util.setBit
import player.phonograph.util.Util.testBit
import player.phonograph.util.Util.unsetBit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.Context

@Composable
fun ClickModeSettingDialogContent(context: Context, onDismiss: () -> Unit) {
    BoxWithConstraints {
        Column(Modifier.heightIn(min = this@BoxWithConstraints.maxHeight * 2 / 3)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Content(context)
            }

            Row(
                modifier = Modifier
                    .height(48.dp)
                    .width(IntrinsicSize.Max)
                    .align(Alignment.End)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Spacer(modifier = Modifier.wrapContentWidth(Alignment.Start))
                Text(
                    text = context.getString(android.R.string.cancel),
                    modifier = Modifier.clickable { onDismiss() },
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun Content(context: Context) {
    Column {

        val currentMode = remember {
            mutableStateOf(Setting.instance.songItemClickMode)
        }
        val setCurrentMode = { new: Int ->
            currentMode.value = new
            Setting.instance.songItemClickMode = new
        }
        for (id in baseModes) {
            ModeRadioBox(
                mode = id,
                name = modeName(context.resources, id),
                currentMode = currentMode,
                setCurrentMode = setCurrentMode
            )
        }

        Spacer(Modifier.height(8.dp))

        val currentExtraFlag = remember {
            mutableStateOf(Setting.instance.songItemClickExtraFlag)
        }
        val flipExtraFlagBit = { mask: Int ->
            val new = if (currentExtraFlag.value.testBit(mask)) {
                currentExtraFlag.value.unsetBit(mask)
            } else {
                currentExtraFlag.value.setBit(mask)
            }
            currentExtraFlag.value = new
            Setting.instance.songItemClickExtraFlag = new
        }

        FlagCheckBox(
            mask = FLAG_MASK_GOTO_POSITION_FIRST,
            name = context.getString(R.string.mode_flag_goto_position_first),
            currentExtraFlag = currentExtraFlag,
            flipExtraFlagBit = flipExtraFlagBit
        )
        FlagCheckBox(
            mask = FLAG_MASK_PLAY_QUEUE_IF_EMPTY,
            name = context.getString(R.string.mode_flag_play_queue_if_empty),
            currentExtraFlag = currentExtraFlag,
            flipExtraFlagBit = flipExtraFlagBit
        )
    }
}


@Composable
fun ModeRadioBox(
    mode: Int,
    name: String,
    currentMode: MutableState<Int>,
    setCurrentMode: (Int) -> Unit,
) {
    Row(Modifier
            .clickable { setCurrentMode(mode) }
            .fillMaxWidth()) {
        RadioButton(selected = currentMode.value == mode, onClick = { setCurrentMode(mode) })
        Text(
            text = name,
            Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
                .alignByBaseline()
        )
    }
}

@Composable
fun FlagCheckBox(
    mask: Int,
    name: String,
    currentExtraFlag: MutableState<Int>,
    flipExtraFlagBit: (Int) -> Unit,
) {

    Row(Modifier
            .clickable { flipExtraFlagBit(mask) }
            .fillMaxWidth()
    ) {
        Checkbox(
            checked = currentExtraFlag.value.testBit(mask),
            onCheckedChange = { flipExtraFlagBit(mask) }
        )
        Text(
            text = name,
            Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
                .alignByBaseline()
        )
    }

}