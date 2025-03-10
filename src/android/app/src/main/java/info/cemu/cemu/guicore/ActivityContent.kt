package info.cemu.cemu.guicore

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import info.cemu.cemu.guicore.theme.CemuTheme

@Composable
fun ActivityContent(content: @Composable () -> Unit) {
    CemuTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}