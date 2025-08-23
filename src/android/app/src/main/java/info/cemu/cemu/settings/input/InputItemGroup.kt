package info.cemu.cemu.settings.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.cemu.cemu.common.ui.components.Header

@Composable
fun InputItemsGroup(
    groupName: String,
    inputIds: List<Int>,
    inputIdToString: (Int) -> String,
    onInputClick: (String, Int) -> Unit,
    controlsMapping: Map<Int, String>,
) {
    Header(groupName)
    inputIds.forEach {
        val buttonName = inputIdToString(it)
        InputItem(
            buttonName = buttonName,
            mapping = controlsMapping[it],
            onClick = { onInputClick(buttonName, it) }
        )
    }
}

@Composable
fun InputItem(
    buttonName: String,
    mapping: String?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = buttonName,
            fontSize = 18.sp,
        )
        Text(
            text = mapping ?: "",
            fontSize = 16.sp
        )
    }
}