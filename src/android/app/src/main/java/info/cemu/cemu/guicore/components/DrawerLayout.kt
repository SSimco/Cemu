package info.cemu.cemu.guicore.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout as AndroidxDrawerLayout

class DrawerLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : AndroidxDrawerLayout(context, attrs) {
    private var isLocked = false
    private val lockedModeDrawerListener = object : DrawerListener {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

        override fun onDrawerOpened(drawerView: View) {
            setDrawerLockMode(LOCK_MODE_UNLOCKED)
        }

        override fun onDrawerClosed(drawerView: View) {
            setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED)
        }

        override fun onDrawerStateChanged(newState: Int) {}
    }

    fun setLockedMode(isLocked: Boolean) {
        if (this.isLocked == isLocked) {
            return
        }

        this.isLocked = isLocked
        if (isLocked) {
            setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED)
            addDrawerListener(lockedModeDrawerListener)
            return
        }

        setDrawerLockMode(LOCK_MODE_UNLOCKED)
        removeDrawerListener(lockedModeDrawerListener)
    }
}