#include "JNIUtils.h"
#include "config/ActiveSettings.h"

extern "C" [[maybe_unused]] JNIEXPORT jstring JNICALL
Java_info_cemu_cemu_nativeinterface_NativeActiveSettings_getMLCPath(JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return JNIUtils::toJString(env, ActiveSettings::GetMlcPath());
}

extern "C" [[maybe_unused]] JNIEXPORT jstring JNICALL
Java_info_cemu_cemu_nativeinterface_NativeActiveSettings_getUserDataPath(JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return JNIUtils::toJString(env, ActiveSettings::GetUserDataPath());
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeActiveSettings_initializeActiveSettings(JNIEnv* env, [[maybe_unused]] jclass clazz, jstring user_data_path, jstring data_path, jstring cache_path)
{
	std::string userDataPath = JNIUtils::toString(env, user_data_path);
	std::string dataPath = JNIUtils::toString(env, data_path);
	std::string cachePath = JNIUtils::toString(env, cache_path);
	std::set<fs::path> failedWriteAccess;
	ActiveSettings::SetPaths(false, {}, userDataPath, userDataPath, cachePath, dataPath, failedWriteAccess);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeActiveSettings_setNativeLibDir(JNIEnv* env, [[maybe_unused]] jclass clazz, jstring native_lib_dir)
{
	ActiveSettings::SetNativeLibPath(JNIUtils::toString(env, native_lib_dir));
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeActiveSettings_setInternalDir(JNIEnv* env, [[maybe_unused]] jclass clazz, jstring internal_dir)
{
	ActiveSettings::SetInternalDir(JNIUtils::toString(env, internal_dir));
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeActiveSettings_hasRequiredOnlineFiles(JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return ActiveSettings::HasRequiredOnlineFiles();
}