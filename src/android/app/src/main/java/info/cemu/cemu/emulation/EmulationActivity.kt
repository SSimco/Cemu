package info.cemu.cemu.emulation

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.text.InputFilter.LengthFilter
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import info.cemu.cemu.BuildConfig
import info.cemu.cemu.R
import info.cemu.cemu.databinding.ActivityEmulationBinding
import info.cemu.cemu.databinding.LayoutSideMenuCheckboxItemBinding
import info.cemu.cemu.databinding.LayoutSideMenuEmulationBinding
import info.cemu.cemu.databinding.LayoutSideMenuTextItemBinding
import info.cemu.cemu.input.InputManager
import info.cemu.cemu.input.SensorManager
import info.cemu.cemu.inputoverlay.InputOverlaySettingsManager
import info.cemu.cemu.inputoverlay.InputOverlaySurfaceView
import info.cemu.cemu.inputoverlay.OverlaySettings
import info.cemu.cemu.nativeinterface.NativeEmulation
import info.cemu.cemu.nativeinterface.NativeException
import info.cemu.cemu.nativeinterface.NativeSwkbd.setCurrentInputText
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
                onEmulationError(getString(R.string.failed_create_surface_error, exception.message))
            }
        }

        override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
            NativeEmulation.clearSurface(isMainCanvas)
            surfaceSet = false
        }
    }

    private val inputManager = InputManager()
    private var emulationTextInputDialog: AlertDialog? = null
    private var isGameRunning = false
    private var padCanvas: SurfaceView? = null
    private var toast: Toast? = null
    private lateinit var binding: ActivityEmulationBinding
    private var isMotionEnabled = false
    private lateinit var overlaySettings: OverlaySettings
    private lateinit var inputOverlaySurfaceView: InputOverlaySurfaceView
    private lateinit var sensorManager: SensorManager
    private var hasEmulationError = false

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (inputManager.onMotionEvent(event)) {
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (inputManager.onKeyEvent(event)) {
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
        emulationActivityInstance = WeakReference(this)

        val inputOverlaySettingsManager = InputOverlaySettingsManager(this)
        overlaySettings = inputOverlaySettingsManager.overlaySettings
        sensorManager = SensorManager(this)

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
        isEnabled: Boolean = true,
        onClick: () -> Unit,
    ) {
        setEnabled(isEnabled)
        textItem.setOnClickListener {
            onClick()
            binding.drawerLayout.close()
        }
    }

    private fun LayoutSideMenuCheckboxItemBinding.configure(
        initialCheckedStatus: Boolean = false,
        onCheckChanged: (Boolean) -> Unit,
    ) {
        checkbox.isChecked = initialCheckedStatus
        checkboxItem.setOnClickListener {
            checkbox.isChecked = !checkbox.isChecked
            onCheckChanged(checkbox.isChecked)
            binding.drawerLayout.close()
        }
    }

    private fun LayoutSideMenuEmulationBinding.configureSideMenu(isInputOverlayEnabled: Boolean) {
        enableMotionCheckbox.configure(onCheckChanged = ::setMotionEnabled)
        replaceTvWithPadCheckbox.configure(onCheckChanged = NativeEmulation::setReplaceTVWithPadView)
        showPadCheckbox.configure(onCheckChanged = ::setPadViewVisibility)
        showInputOverlayCheckbox.configure(initialCheckedStatus = isInputOverlayEnabled) { showInputOverlay ->
            editInputsMenuItem.setEnabled(showInputOverlay)
            resetInputOverlayMenuItem.setEnabled(showInputOverlay)
            inputOverlaySurfaceView.setVisible(showInputOverlay)
        }
        editInputsMenuItem.configure(isEnabled = isInputOverlayEnabled) {
            binding.editInputsLayout.visibility = View.VISIBLE
            binding.finishEditInputsButton.visibility = View.VISIBLE
            binding.moveInputsButton.performClick()
        }
        resetInputOverlayMenuItem.configure(
            isEnabled = isInputOverlayEnabled,
            onClick = inputOverlaySurfaceView::resetInputs
        )
        exitMenuItem.configure(onClick = onBackPressedDispatcher::onBackPressed)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeView(launchPath: String) {
        setFullscreen()

        binding = ActivityEmulationBinding.inflate(layoutInflater)
        inputOverlaySurfaceView = binding.inputOverlay

        binding.sideMenu.configureSideMenu(overlaySettings.isOverlayEnabled)

        binding.moveInputsButton.setOnClickListener { _ ->
            if (inputOverlaySurfaceView.getInputMode() == InputOverlaySurfaceView.InputMode.EDIT_POSITION) {
                return@setOnClickListener
            }
            binding.resizeInputsButton.alpha = 0.5f
            binding.moveInputsButton.alpha = 1.0f
            toastMessage(R.string.input_mode_edit_position)
            inputOverlaySurfaceView.setInputMode(InputOverlaySurfaceView.InputMode.EDIT_POSITION)
        }
        binding.resizeInputsButton.setOnClickListener { _ ->
            if (inputOverlaySurfaceView.getInputMode() == InputOverlaySurfaceView.InputMode.EDIT_SIZE) {
                return@setOnClickListener
            }
            binding.moveInputsButton.alpha = 0.5f
            binding.resizeInputsButton.alpha = 1.0f
            toastMessage(R.string.input_mode_edit_size)
            inputOverlaySurfaceView.setInputMode(InputOverlaySurfaceView.InputMode.EDIT_SIZE)
        }
        binding.finishEditInputsButton.setOnClickListener { _ ->
            inputOverlaySurfaceView.setInputMode(InputOverlaySurfaceView.InputMode.DEFAULT)
            binding.finishEditInputsButton.visibility = View.GONE
            binding.editInputsLayout.visibility = View.GONE
            toastMessage(R.string.input_mode_default)
        }
        binding.emulationSettingsButton.setOnClickListener { binding.drawerLayout.open() }
        val mainCanvas = binding.mainCanvas
        try {
            val testSurfaceTexture = SurfaceTexture(0)
            val testSurface = Surface(testSurfaceTexture)
            NativeEmulation.initializeRenderer(testSurface)
            testSurface.release()
            testSurfaceTexture.release()
        } catch (exception: NativeException) {
            onEmulationError(
                getString(
                    R.string.failed_initialize_renderer_error,
                    exception.message
                )
            )
            return
        }

        val mainCanvasHolder = mainCanvas.holder
        mainCanvasHolder.addCallback(CanvasSurfaceHolderCallback(isMainCanvas = true))
        mainCanvasHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {
                if (hasEmulationError) {
                    return
                }
                if (!isGameRunning) {
                    isGameRunning = true
                    startGame(launchPath)
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }
        })
        mainCanvas.setOnTouchListener(CanvasOnTouchListener(isTV = true))
    }

    private fun toastMessage(@StringRes toastTextResId: Int) {
        toast?.cancel()
        toast = Toast.makeText(this, toastTextResId, Toast.LENGTH_SHORT)
            .also { it.show() }
    }

    private fun startGame(launchPath: String) {
        val result = NativeEmulation.startGame(launchPath)
        if (result == NativeEmulation.START_GAME_SUCCESSFUL) {
            return
        }
        val errorMessage = when (result) {
            NativeEmulation.START_GAME_ERROR_GAME_BASE_FILES_NOT_FOUND -> getString(R.string.game_not_found)
            NativeEmulation.START_GAME_ERROR_NO_DISC_KEY -> getString(R.string.no_disk_key)
            NativeEmulation.START_GAME_ERROR_NO_TITLE_TIK -> getString(R.string.no_title_tik)
            else -> getString(R.string.game_files_unknown_error, launchPath)
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
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.exit_confirmation_title)
            .setMessage(R.string.exit_confirm_message)
            .setPositiveButton(R.string.yes) { _, _ -> quit() }
            .setNegativeButton(R.string.no) { _, _ -> }
            .show()
    }

    private fun onEmulationError(emulationError: String?) {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.error)
            .setMessage(emulationError)
            .setNeutralButton(R.string.quit) { _, _ -> }
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
            setCurrentInputText(initialText)
            emulationActivity.runOnUiThread {
                val inputEditTextLayout =
                    emulationActivity.layoutInflater.inflate(
                        R.layout.layout_emulation_input,
                        null
                    )
                val inputEditText =
                    inputEditTextLayout.requireViewById<EmulationTextInputEditText>(R.id.emulation_input_text)
                inputEditText.updateText(initialText)
                val dialog = MaterialAlertDialogBuilder(emulationActivity)
                    .setView(inputEditTextLayout)
                    .setCancelable(false)
                    .setPositiveButton(R.string.done) { _, _ -> }.show()
                val doneButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)!!
                doneButton.isEnabled = false
                doneButton.setOnClickListener { _ -> inputEditText.onFinishedEdit() }
                inputEditText.setOnTextChangedListener {
                    doneButton.isEnabled = it.isNotEmpty()
                }
                val parentTextInputLayout =
                    inputEditTextLayout.requireViewById<TextInputLayout>(R.id.emulation_input_layout)
                if (maxLength > 0) {
                    parentTextInputLayout.isCounterEnabled = true
                    parentTextInputLayout.counterMaxLength = maxLength
                    inputEditText.appendFilter(LengthFilter(maxLength))
                } else {
                    parentTextInputLayout.isCounterEnabled = false
                }
                emulationActivity.emulationTextInputDialog = dialog
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