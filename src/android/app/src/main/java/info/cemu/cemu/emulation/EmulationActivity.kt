package info.cemu.cemu.emulation

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import info.cemu.cemu.BuildConfig
import info.cemu.cemu.common.translation.tr
import info.cemu.cemu.databinding.ActivityEmulationBinding
import info.cemu.cemu.databinding.LayoutSideMenuCheckboxItemBinding
import info.cemu.cemu.databinding.LayoutSideMenuEmulationBinding
import info.cemu.cemu.databinding.LayoutSideMenuTextItemBinding
import info.cemu.cemu.nativeinterface.NativeEmulation
import info.cemu.cemu.nativeinterface.NativeException
import info.cemu.cemu.common.settings.EmulationScreenSettings
import info.cemu.cemu.common.settings.InputOverlaySettings
import info.cemu.cemu.common.settings.SettingsManager
import info.cemu.cemu.emulation.inputoverlay.InputOverlaySurfaceView
import java.lang.ref.WeakReference
import kotlin.system.exitProcess

class EmulationActivity : AppCompatActivity() {
    private inner class CanvasSurfaceHolderCallback(val isMainCanvas: Boolean) :
        SurfaceHolder.Callback {
        var surfaceSet: Boolean = false

        override fun surfaceCreated(surfaceHolder: SurfaceHolder) {}

        override fun surfaceChanged(
            surfaceHolder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {
            try {
                NativeEmulation.setSurfaceSize(width, height, isMainCanvas)
                if (surfaceSet) {
                    return
                }
                NativeEmulation.setSurface(surfaceHolder.surface, isMainCanvas)
                surfaceSet = true
            } catch (exception: NativeException) {
                onEmulationError(tr("Failed creating surface: {0}", exception.message!!))
            }
        }

        override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
            NativeEmulation.clearSurface(isMainCanvas)
            surfaceSet = false
        }
    }

    private var emulationTextInputDialog: AlertDialog? = null
    private var isGameRunning = false
    private var padCanvas: SurfaceView? = null
    private var toast: Toast? = null
    private lateinit var binding: ActivityEmulationBinding
    private var isMotionEnabled = false
    private lateinit var inputOverlaySettings: InputOverlaySettings
    private lateinit var emulationScreenSettings: EmulationScreenSettings
    private lateinit var inputOverlaySurfaceView: InputOverlaySurfaceView
    private lateinit var sensorManager: SensorManager
    private var hasEmulationError = false

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (InputHandler.onMotionEvent(event)) {
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (InputHandler.onKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun getLaunchPath(): String {
        val extras = intent.extras
        val data = intent.data
        var launchPath: String? = null
        if (extras != null) {
            launchPath = extras.getString(EXTRA_LAUNCH_PATH)
        }
        if (launchPath == null && data != null) {
            launchPath = data.toString()
        }
        if (launchPath == null) {
            throw RuntimeException("launchPath is null")
        }
        return launchPath
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        emulationActivityInstance = WeakReference(this)

        inputOverlaySettings = SettingsManager.inputOverlaySettings
        emulationScreenSettings = SettingsManager.emulationScreenSettings
        sensorManager = SensorManager(this)
        sensorManager.setDeviceRotationProvider(deviceRotationProvider = { display.rotation })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })

        initializeView(getLaunchPath())
        setContentView(binding.root)
    }

    private fun destroyPadCanvas() {
        if (padCanvas == null) {
            return
        }
        binding.canvasesLayout.removeView(padCanvas)
        padCanvas = null
    }

    private fun setPadViewVisibility(visible: Boolean) {
        if (visible) {
            createPadCanvas()
        } else {
            destroyPadCanvas()
        }
    }

    private fun setMotionEnabled(enabled: Boolean) {
        isMotionEnabled = enabled
        if (isMotionEnabled) {
            sensorManager.startListening()
        } else {
            sensorManager.pauseListening()
        }
    }

    private fun LayoutSideMenuTextItemBinding.setEnabled(isEnabled: Boolean) {
        textItem.isEnabled = isEnabled
        textItem.alpha = if (isEnabled) 1f else 0.7f
    }

    private fun LayoutSideMenuTextItemBinding.configure(
        label: String,
        isEnabled: Boolean = true,
        onClick: () -> Unit,
    ) {
        setEnabled(isEnabled)
        this.label = label
        textItem.setOnClickListener {
            onClick()
            binding.drawerLayout.close()
        }
    }

    private fun LayoutSideMenuCheckboxItemBinding.configure(
        label: String,
        initialCheckedStatus: Boolean = false,
        onCheckChanged: (Boolean) -> Unit,
    ) {
        this.label = label
        checkbox.isChecked = initialCheckedStatus
        checkboxItem.setOnClickListener {
            checkbox.isChecked = !checkbox.isChecked
            onCheckChanged(checkbox.isChecked)
            binding.drawerLayout.close()
        }
    }

    private fun LayoutSideMenuEmulationBinding.configureSideMenu() {
        val isInputOverlayEnabled = inputOverlaySettings.isOverlayEnabled
        enableMotionCheckbox.configure(
            label = tr("Enable motion"),
            onCheckChanged = ::setMotionEnabled
        )
        replaceTvWithPadCheckbox.configure(
            label = tr("Replace TV with PAD"),
            onCheckChanged = NativeEmulation::setReplaceTVWithPadView
        )
        showPadCheckbox.configure(
            label = tr(text = "Show PAD"),
            onCheckChanged = ::setPadViewVisibility
        )
        showInputOverlayCheckbox.configure(
            tr(text = "Show input overlay"),
            initialCheckedStatus = isInputOverlayEnabled,
            onCheckChanged = { showInputOverlay ->
                editInputsMenuItem.setEnabled(showInputOverlay)
                resetInputOverlayMenuItem.setEnabled(showInputOverlay)
                inputOverlaySurfaceView.setVisible(showInputOverlay)
            }
        )
        editInputsMenuItem.configure(
            label = tr("Edit inputs"),
            isEnabled = isInputOverlayEnabled,
            onClick = {
                binding.editInputsLayout.visibility = View.VISIBLE
                binding.finishEditInputsButton.visibility = View.VISIBLE
                binding.moveInputsButton.performClick()
            })
        resetInputOverlayMenuItem.configure(
            tr(text = "Reset input overlay"),
            isEnabled = isInputOverlayEnabled,
            onClick = inputOverlaySurfaceView::resetInputs
        )
        exitMenuItem.configure(tr("Exit"), onClick = onBackPressedDispatcher::onBackPressed)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeView(launchPath: String) {
        setFullscreen()

        binding = ActivityEmulationBinding.inflate(layoutInflater)

        initializeInputOverlay()

        binding.sideMenu.configureSideMenu()

        binding.moveInputsButton.setOnClickListener { _ ->
            if (inputOverlaySurfaceView.getInputMode() == InputOverlaySurfaceView.InputMode.EDIT_POSITION) {
                return@setOnClickListener
            }
            binding.resizeInputsButton.alpha = 0.5f
            binding.moveInputsButton.alpha = 1.0f
            toastMessage(tr("Edit input positions"))
            inputOverlaySurfaceView.setInputMode(InputOverlaySurfaceView.InputMode.EDIT_POSITION)
        }
        binding.resizeInputsButton.setOnClickListener { _ ->
            if (inputOverlaySurfaceView.getInputMode() == InputOverlaySurfaceView.InputMode.EDIT_SIZE) {
                return@setOnClickListener
            }
            binding.moveInputsButton.alpha = 0.5f
            binding.resizeInputsButton.alpha = 1.0f
            toastMessage(tr("Edit input size"))
            inputOverlaySurfaceView.setInputMode(InputOverlaySurfaceView.InputMode.EDIT_SIZE)
        }
        binding.finishEditInputsButton.text = tr("Done")
        binding.finishEditInputsButton.setOnClickListener { _ ->
            inputOverlaySurfaceView.setInputMode(InputOverlaySurfaceView.InputMode.DEFAULT)
            binding.finishEditInputsButton.visibility = View.GONE
            binding.editInputsLayout.visibility = View.GONE
            toastMessage(tr("Exited input edit mode"))
        }

        if (emulationScreenSettings.isDrawerButtonVisible) {
            binding.emulationSettingsButton.setOnClickListener { binding.drawerLayout.open() }
            binding.drawerLayout.setLockedMode(true)
        } else {
            binding.emulationSettingsButton.visibility = View.GONE
        }

        val mainCanvas = binding.mainCanvas
        try {
            val testSurfaceTexture = SurfaceTexture(0)
            val testSurface = Surface(testSurfaceTexture)
            NativeEmulation.initializeRenderer(testSurface)
            testSurface.release()
            testSurfaceTexture.release()
        } catch (exception: NativeException) {
            onEmulationError(tr("Failed to initialize renderer: {0}", exception.message!!))
            return
        }

        val mainCanvasHolder = mainCanvas.holder
        mainCanvasHolder.addCallback(CanvasSurfaceHolderCallback(isMainCanvas = true))
        mainCanvasHolder.addCallback(object : SurfaceChangedListener() {
            override fun surfaceChanged() {
                if (hasEmulationError) {
                    return
                }
                if (!isGameRunning) {
                    isGameRunning = true
                    startGame(launchPath)
                }
            }
        })
        mainCanvas.setOnTouchListener(CanvasOnTouchListener(isTV = true))
    }

    private fun initializeInputOverlay() {
        inputOverlaySurfaceView = binding.inputOverlay

        inputOverlaySurfaceView.setVisible(inputOverlaySettings.isOverlayEnabled)
    }

    private fun toastMessage(text: String) {
        toast?.cancel()
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
            .also { it.show() }
    }

    private fun startGame(launchPath: String) {
        val result = NativeEmulation.startGame(launchPath)
        if (result == NativeEmulation.StartGameStatusCode.SUCCESSFUL) {
            return
        }
        val errorMessage = when (result) {
            NativeEmulation.StartGameStatusCode.ERROR_GAME_BASE_FILES_NOT_FOUND -> tr("Unable to launch game because the base files were not found.")
            NativeEmulation.StartGameStatusCode.ERROR_NO_DISC_KEY -> tr("Could not decrypt title. Make sure that keys.txt contains the correct disc key for this title.")
            NativeEmulation.StartGameStatusCode.ERROR_NO_TITLE_TIK -> tr("Could not decrypt title because title.tik is missing.")
            else -> tr("Unable to launch game\nPath: {0}", launchPath)
        }

        onEmulationError(errorMessage)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.pauseListening()
    }

    override fun onResume() {
        super.onResume()
        if (isMotionEnabled) {
            sensorManager.startListening()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        sensorManager.pauseListening()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createPadCanvas() {
        if (padCanvas != null) {
            return
        }
        val padCanvas = SurfaceView(this)
        binding.canvasesLayout.addView(
            padCanvas,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f)
        )
        padCanvas.holder.addCallback(CanvasSurfaceHolderCallback(false))
        padCanvas.setOnTouchListener(CanvasOnTouchListener(false))
        this.padCanvas = padCanvas
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(tr("Exit confirmation"))
            .setMessage(tr("Are you sure you want to exit?"))
            .setPositiveButton(tr("Yes")) { _, _ -> quit() }
            .setNegativeButton(tr("No")) { _, _ -> }
            .show()
    }

    private fun onEmulationError(emulationError: String?) {
        MaterialAlertDialogBuilder(this)
            .setTitle(tr("Error"))
            .setMessage(emulationError)
            .setNeutralButton(tr("Quit")) { _, _ -> }
            .setOnDismissListener { _ -> quit() }
            .show()
    }

    private fun setFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun quit() {
        finishAffinity()
        exitProcess(0)
    }

    companion object {
        const val EXTRA_LAUNCH_PATH: String = BuildConfig.APPLICATION_ID + ".LaunchPath"
        private var emulationActivityInstance: WeakReference<EmulationActivity?> =
            WeakReference(null)

        /**
         * This method is called by swkbd using JNI.
         */
        @Keep
        @JvmStatic
        fun showEmulationTextInput(initialText: String?, maxLength: Int) {
            val emulationActivity = emulationActivityInstance.get() ?: return
            if (emulationActivity.emulationTextInputDialog != null) {
                return
            }

            emulationActivity.runOnUiThread {
                emulationActivity.emulationTextInputDialog = showEmulationTextInputDialog(
                    initialText,
                    maxLength,
                    emulationActivity,
                    emulationActivity.layoutInflater
                )
            }
        }

        /**
         * This method is called by swkbd using JNI.
         */
        @Keep
        @JvmStatic
        fun hideEmulationTextInput() {
            val emulationActivity = emulationActivityInstance.get() ?: return
            val textInputDialog = emulationActivity.emulationTextInputDialog ?: return
            emulationActivity.emulationTextInputDialog = null
            emulationActivity.runOnUiThread { textInputDialog.dismiss() }
        }
    }
}