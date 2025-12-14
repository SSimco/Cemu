package info.cemu.cemu.emulation

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import info.cemu.cemu.BuildConfig
import info.cemu.cemu.common.ui.components.ActivityContent
import info.cemu.cemu.common.ui.localization.TranslatableContent
import kotlin.system.exitProcess

class EmulationActivity : AppCompatActivity() {
    private lateinit var sensorManager: SensorManager

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

    private fun getGamePath(): String {
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
        sensorManager = SensorManager(this)
        sensorManager.setDeviceRotationProvider { display.rotation }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setFullscreen()

        val gamePath = getGamePath()

        setContent {
            TranslatableContent {
                ActivityContent {
                    EmulationScreen(
                        gamePath = gamePath,
                        setMotionSensorEnabled = sensorManager::setIsListening,
                        onQuit = ::onQuit,
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.pauseListening()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.resumeListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.pauseListening()
    }

    private fun setFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun onQuit() {
        finish()
        exitProcess(0)
    }

    companion object {
        const val EXTRA_LAUNCH_PATH: String = BuildConfig.APPLICATION_ID + ".LaunchPath"
    }
}
