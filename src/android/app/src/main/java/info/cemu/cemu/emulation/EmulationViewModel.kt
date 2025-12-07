package info.cemu.cemu.emulation

import android.view.SurfaceHolder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.cemu.cemu.common.result.failure
import info.cemu.cemu.common.result.then
import info.cemu.cemu.common.result.thenRun
import info.cemu.cemu.common.settings.SettingsManager
import info.cemu.cemu.common.ui.localization.tr
import info.cemu.cemu.nativeinterface.NativeEmulation
import info.cemu.cemu.nativeinterface.NativeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SideMenuState(
    val isMotionEnabled: Boolean = false,
    val isDrawerLocked: Boolean = false,
    val isTVReplacedWithPad: Boolean = false,
    val isPadVisible: Boolean = false,
    val isInputOverlayVisible: Boolean = false,
)

data class SurfacesConfig(
    val isVertical: Boolean,
    val isReversed: Boolean,
) {
    companion object {
        fun createFromSettings(): SurfacesConfig {
            val position = SettingsManager.emulationSettings.gamePadPosition

            return SurfacesConfig(
                isVertical = position.isVertical(),
                isReversed = position.appearsAfterTV(),
            )
        }
    }
}

class ConditionFlags(
    var isMainConditionMet: Boolean = false,
    var isPadConditionMet: Boolean = false
) {
    fun get(isMain: Boolean): Boolean {
        return if (isMain) isMainConditionMet else isPadConditionMet
    }

    fun set(isMain: Boolean, value: Boolean) {
        if (isMain) {
            isMainConditionMet = value
        } else {
            isPadConditionMet = value
        }
    }
}


class EmulationViewModel(private val launchPath: String) : ViewModel() {
    private val _emulationError = MutableStateFlow<String?>(null)
    val emulationError = _emulationError.asStateFlow()

    private val _sideMenuState = MutableStateFlow(
        SideMenuState(
            isInputOverlayVisible = SettingsManager.inputOverlaySettings.isOverlayEnabled
        )
    )
    val sideMenuState = _sideMenuState.asStateFlow()

    fun updateSideMenuState(sideMenuState: SideMenuState) {
        _sideMenuState.value = sideMenuState
    }

    val surfacesConfig = SurfacesConfig.createFromSettings()

    val destroyedSurfaces = ConditionFlags()
    var setSurfaces = ConditionFlags()

    private inner class CanvasSurfaceHolderCallback(val isMainCanvas: Boolean) :
        SurfaceHolder.Callback {

        override fun surfaceCreated(surfaceHolder: SurfaceHolder) {}

        override fun surfaceChanged(
            surfaceHolder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {
            try {
                NativeEmulation.setSurfaceSize(width, height, isMainCanvas)

                if (setSurfaces.get(isMainCanvas)) {
                    return
                }

                NativeEmulation.setSurface(surfaceHolder.surface, isMainCanvas)
                val mainSurfaceWasDestroyed = destroyedSurfaces.get(isMain = true)

                if (mainSurfaceWasDestroyed && isMainCanvas) {
                    NativeEmulation.resumeTitle()
                }

                setSurfaces.set(isMainCanvas, true)

                val padSurfaceWasSet = setSurfaces.get(isMain = false)
                if ((!isMainCanvas && !mainSurfaceWasDestroyed) || (isMainCanvas && padSurfaceWasSet)) {
                    NativeEmulation.initializeSurface(isMainCanvas = false)
                }

                destroyedSurfaces.set(isMainCanvas, false)
            } catch (exception: NativeException) {
                _emulationError.value = tr("Failed creating surface: {0}", exception.message!!)
            }
        }

        override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
            if (setSurfaces.get(isMain = false)) {
                NativeEmulation.clearPadSurface()
                setSurfaces.set(isMain = false, false)
                destroyedSurfaces.set(isMain = false, true)
            }

            if (isMainCanvas) {
                NativeEmulation.pauseTitle()

                setSurfaces.set(isMain = true, false)
                destroyedSurfaces.set(isMain = true, true)
            }
        }
    }

    val mainHolderCallback: SurfaceHolder.Callback = CanvasSurfaceHolderCallback(true)
    val padHolderCallback: SurfaceHolder.Callback = CanvasSurfaceHolderCallback(false)

    private suspend fun initializeSystems() {
        return withContext(Dispatchers.IO) {
            NativeEmulation.initializeSystems()
        }
    }

    private suspend fun initializeRenderer(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                NativeEmulation.initializeRenderer()

                NativeEmulation.initializeSurface(isMainCanvas = true)

                return@withContext Result.success(Unit)

            } catch (exception: NativeException) {
                val errorMessage = tr("Failed creating renderer: {0}", exception.message!!)
                return@withContext Result.failure(errorMessage)
            }
        }
    }

    private suspend fun prepareTitle(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val result = NativeEmulation.prepareTitle(launchPath)

            if (result == NativeEmulation.PrepareTitleResult.SUCCESSFUL) {
                return@withContext Result.success(Unit)
            }

            val errorMessage = when (result) {
                NativeEmulation.PrepareTitleResult.ERROR_GAME_BASE_FILES_NOT_FOUND -> tr("Unable to launch game because the base files were not found.")
                NativeEmulation.PrepareTitleResult.ERROR_NO_DISC_KEY -> tr("Could not decrypt title. Make sure that keys.txt contains the correct disc key for this title.")
                NativeEmulation.PrepareTitleResult.ERROR_NO_TITLE_TIK -> tr("Could not decrypt title because title.tik is missing.")
                else -> tr("Unable to launch game\nPath: {0}", launchPath)
            }

            return@withContext Result.failure(errorMessage)
        }
    }


    private suspend fun launchTitle() {
        return withContext(Dispatchers.IO) {
            NativeEmulation.launchTitle()
        }
    }

    private val _isEmulationInitialized = MutableStateFlow(false)
    val isEmulationInitialized = _isEmulationInitialized.asStateFlow()
    private var emulationInitializationJob: Job? = null
    fun initializeEmulation() {
        if (_isEmulationInitialized.value || emulationInitializationJob != null) {
            return
        }

        emulationInitializationJob = viewModelScope.launch {
            prepareTitle()
                .thenRun { initializeSystems() }
                .then { initializeRenderer() }
                .thenRun { launchTitle() }
                .onFailure { _emulationError.value = it.message }

            _isEmulationInitialized.value = true
        }
    }

    companion object {
        val LAUNCH_PATH_KEY = object : CreationExtras.Key<String> {}
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EmulationViewModel(
                    this[LAUNCH_PATH_KEY] as String
                )
            }
        }
    }
}