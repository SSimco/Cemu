#include "AndroidSwkbdCallbacks.h"
#include "JNIUtils.h"

AndroidSwkbdCallbacks::AndroidSwkbdCallbacks()
{
	JNIUtils::ScopedJNIENV env;
	m_emulationActivityClass = JNIUtils::Scopedjclass("info/cemu/cemu/emulation/EmulationActivity");
	m_showSoftwareKeyboardMethodID = env->GetStaticMethodID(*m_emulationActivityClass, "showEmulationTextInput", "(Ljava/lang/String;I)V");
	m_hideSoftwareKeyboardMethodID = env->GetStaticMethodID(*m_emulationActivityClass, "hideEmulationTextInput", "()V");
}

void AndroidSwkbdCallbacks::showSoftwareKeyboard(const std::string& initialText, sint32 maxLength)
{
	JNIUtils::fiberSafeJNICall([&](JNIEnv* env) {
		jstring j_initialText = JNIUtils::toJString(env, initialText);
		JNIUtils::ScopedJNIENV()->CallStaticVoidMethod(*m_emulationActivityClass, m_showSoftwareKeyboardMethodID, j_initialText, maxLength);
		env->DeleteLocalRef(j_initialText);
	});
}

void AndroidSwkbdCallbacks::hideSoftwareKeyboard()
{
	JNIUtils::fiberSafeJNICall([&](JNIEnv* env) {
		env->CallStaticVoidMethod(*m_emulationActivityClass, m_hideSoftwareKeyboardMethodID);
	});
}
