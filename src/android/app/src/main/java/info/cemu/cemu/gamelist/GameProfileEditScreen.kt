package info.cemu.cemu.gamelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.cemu.cemu.common.ui.components.Header
import info.cemu.cemu.common.ui.components.ScreenContent
import info.cemu.cemu.common.ui.components.SingleSelection
import info.cemu.cemu.common.ui.components.Toggle
import info.cemu.cemu.common.ui.localization.tr
import info.cemu.cemu.nativeinterface.NativeGameTitles

@Composable
fun GameProfileEditScreen(game: NativeGameTitles.Game?, navigateBack: () -> Unit) {
    if (game == null)
        return

    val titleId = game.titleId
    ScreenContent(
        appBarText = tr("Edit game profile"),
        navigateBack = navigateBack,
        contentModifier = Modifier.padding(16.dp),
        contentVerticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header(text = game.name)
        Toggle(
            label = tr("Load shared libraries"),
            description = tr("Load libraries from the cafeLibs directory"),
            initialCheckedState = {
                NativeGameTitles.isLoadingSharedLibrariesForTitleEnabled(
                    titleId
                )
            },
            onCheckedChanged = { enabled ->
                NativeGameTitles.setLoadingSharedLibrariesForTitleEnabled(
                    titleId,
                    enabled
                )
            },
        )
        Toggle(
            label = tr("Shader multiplication accuracy"),
            description = tr("Controls the accuracy of floating point multiplication in shaders"),
            initialCheckedState = {
                NativeGameTitles.isShaderMultiplicationAccuracyForTitleEnabled(
                    titleId
                )
            },
            onCheckedChanged = { enabled ->
                NativeGameTitles.setShaderMultiplicationAccuracyForTitleEnabled(
                    titleId,
                    enabled
                )
            },
        )
        SingleSelection(
            label = tr("CPU mode"),
            initialChoice = { NativeGameTitles.getCpuModeForTitle(titleId) },
            choices = listOf(
                NativeGameTitles.CPUMode.SINGLECOREINTERPRETER,
                NativeGameTitles.CPUMode.SINGLECORERECOMPILER,
                NativeGameTitles.CPUMode.MULTICORERECOMPILER,
                NativeGameTitles.CPUMode.AUTO
            ),
            choiceToString = { cpuMode -> cpuModeToString(cpuMode) },
            onChoiceChanged = { cpuMode -> NativeGameTitles.setCpuModeForTitle(titleId, cpuMode) }
        )
        SingleSelection(
            label = tr("Thread quantum"),
            initialChoice = { NativeGameTitles.getThreadQuantumForTitle(titleId) },
            choices = NativeGameTitles.THREAD_QUANTUM_VALUES.toList(),
            choiceToString = { it.toString() },
            onChoiceChanged = { threadQuantum ->
                NativeGameTitles.setThreadQuantumForTitle(titleId, threadQuantum)
            }
        )
    }
}

private fun cpuModeToString(cpuMode: Int): String = when (cpuMode) {
    NativeGameTitles.CPUMode.SINGLECOREINTERPRETER -> tr("Single-core interpreter")
    NativeGameTitles.CPUMode.SINGLECORERECOMPILER -> tr("Single-core recompiler")
    NativeGameTitles.CPUMode.MULTICORERECOMPILER -> tr("Multi-core recompiler")
    else -> tr("Auto (recommended)")
}
