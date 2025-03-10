package info.cemu.cemu.guicore.components

import androidx.annotation.IntRange
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.material3.Slider as MaterialSlider

@Composable
fun Slider(
    label: String,
    initialValue: () -> Int,
    valueFrom: Int,
    valueTo: Int,
    @IntRange(from = 0) steps: Int = 0,
    labelFormatter: (Int) -> String,
    onValueChange: (Int) -> Unit,
) {
    var value by rememberSaveable { mutableFloatStateOf(initialValue().toFloat()) }
    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.padding(end = 8.dp)) {
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
            Text(
                text = labelFormatter(value.fastRoundToInt()),
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
            )
        }
        MaterialSlider(
            valueRange = valueFrom.toFloat()..valueTo.toFloat(),
            steps = steps,
            value = value,
            onValueChangeFinished = { onValueChange(value.toInt()) },
            onValueChange = { value = it },
        )
    }
}