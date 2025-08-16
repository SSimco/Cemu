package info.cemu.cemu.emulation

import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import info.cemu.cemu.common.translation.tr
import info.cemu.cemu.nativeinterface.NativeSwkbd
import java.util.regex.Pattern

class EmulationTextInputEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle,
) : TextInputEditText(context, attrs, defStyleAttr) {
    fun appendFilter(inputFilter: InputFilter) {
        filters += inputFilter
    }

    fun updateText(text: String?) {
        val hasFocus = hasFocus()
        if (hasFocus) {
            clearFocus()
        }
        setText(text)
        if (hasFocus) {
            requestFocus()
        }
    }

    private var onTextChangedListener: ((CharSequence) -> Unit)? = null

    init {
        hint = tr("Input text")

        appendFilter { source: CharSequence, _, _, _, _, _ ->
            if (INPUT_PATTERN.matcher(source).matches()) null else ""
        }
        inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_NORMAL
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
                if (!hasFocus()) {
                    return
                }
                NativeSwkbd.onTextChanged(text.toString())
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    override fun onEditorAction(actionCode: Int) {
        if (actionCode == EditorInfo.IME_ACTION_DONE && !text.isNullOrEmpty()) {
            onFinishedEdit()
        }
        super.onEditorAction(actionCode)
    }

    fun onFinishedEdit() {
        NativeSwkbd.onFinishedInputEdit()
    }

    fun setOnTextChangedListener(onTextChangedListener: ((CharSequence) -> Unit)?) {
        this.onTextChangedListener = onTextChangedListener
    }

    companion object {
        private val INPUT_PATTERN: Pattern =
            Pattern.compile("^[\\da-zA-Z \\-/;:',.?!#\\[\\]$%^&*()_@\\\\<>+=]+$")
    }
}

fun showEmulationTextInputDialog(
    initialText: String?,
    maxLength: Int,
    context: Context,
    layoutInflater: LayoutInflater
): AlertDialog {
    NativeSwkbd.setCurrentInputText(initialText)

    val inputEditTextLayout =
        layoutInflater.inflate(
            info.cemu.cemu.R.layout.layout_emulation_input,
            null
        )

    val inputEditText =
        inputEditTextLayout.requireViewById<EmulationTextInputEditText>(info.cemu.cemu.R.id.emulation_input_text)

    inputEditText.updateText(initialText)

    val dialog = MaterialAlertDialogBuilder(context)
        .setView(inputEditTextLayout)
        .setCancelable(false)
        .setPositiveButton(tr("Done")) { _, _ -> }
        .show()

    val doneButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)!!

    doneButton.isEnabled = false

    doneButton.setOnClickListener { _ -> inputEditText.onFinishedEdit() }

    inputEditText.setOnTextChangedListener {
        doneButton.isEnabled = it.isNotEmpty()
    }

    val parentTextInputLayout =
        inputEditTextLayout.requireViewById<TextInputLayout>(info.cemu.cemu.R.id.emulation_input_layout)

    if (maxLength > 0) {
        parentTextInputLayout.isCounterEnabled = true
        parentTextInputLayout.counterMaxLength = maxLength
        inputEditText.appendFilter(LengthFilter(maxLength))
    } else {
        parentTextInputLayout.isCounterEnabled = false
    }

    return dialog
}