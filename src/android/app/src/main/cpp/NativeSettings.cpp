#include "JNIUtils.h"
#include "audio/IAudioAPI.h"
#include "config/CemuConfig.h"

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getOverlayPosition([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return static_cast<jint>(g_config.data().overlay.position);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setOverlayPosition([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint position)
{
	g_config.data().overlay.position = static_cast<ScreenPosition>(position);
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getOverlayTextScalePercentage([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().overlay.text_scale;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setOverlayTextScalePercentage([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint scalePercentage)
{
	g_config.data().overlay.text_scale = scalePercentage;
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_isOverlayFPSEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().overlay.fps;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setOverlayFPSEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean enabled)
{
	g_config.data().overlay.fps = enabled;
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_isOverlayDrawCallsPerFrameEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().overlay.drawcalls;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setOverlayDrawCallsPerFrameEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean enabled)
{
	g_config.data().overlay.drawcalls = enabled;
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_isOverlayCPUUsageEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().overlay.cpu_usage;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setOverlayCPUUsageEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean enabled)
{
	g_config.data().overlay.cpu_usage = enabled;
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_isOverlayCPUPerCoreUsageEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().overlay.cpu_per_core_usage;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setOverlayCPUPerCoreUsageEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean enabled)
{
	g_config.data().overlay.cpu_per_core_usage = enabled;
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_isOverlayRAMUsageEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().overlay.ram_usage;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setOverlayRAMUsageEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean enabled)
{
	g_config.data().overlay.ram_usage = enabled;
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_isOverlayVRAMUsageEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().overlay.vram_usage;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setOverlayVRAMUsageEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean enabled)
{
	g_config.data().overlay.vram_usage = enabled;
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_isOverlayDebugEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().overlay.debug;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setOverlayDebugEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean enabled)
{
	g_config.data().overlay.debug = enabled;
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getNotificationsPosition([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return static_cast<jint>(g_config.data().notification.position);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setNotificationsPosition([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint position)
{
	g_config.data().notification.position = static_cast<ScreenPosition>(position);
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getNotificationsTextScalePercentage([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().notification.text_scale;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setNotificationsTextScalePercentage([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint scalePercentage)
{
	g_config.data().notification.text_scale = scalePercentage;
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_isNotificationControllerProfilesEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().notification.controller_profiles;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setNotificationControllerProfilesEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean enabled)
{
	g_config.data().notification.controller_profiles = enabled;
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_isNotificationShaderCompilerEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().notification.shader_compiling;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setNotificationShaderCompilerEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean enabled)
{
	g_config.data().notification.shader_compiling = enabled;
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_isNotificationFriendListEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().notification.friends;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setNotificationFriendListEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean enabled)
{
	g_config.data().notification.friends = enabled;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_addGamesPath(JNIEnv* env, [[maybe_unused]] jclass clazz, jstring uri)
{
	auto& gamePaths = g_config.data().game_paths;
	auto gamePath = JNIUtils::toString(env, uri);
	if (std::any_of(gamePaths.begin(), gamePaths.end(), [&](auto path) { return path == gamePath; }))
		return;
	gamePaths.push_back(gamePath);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_removeGamesPath(JNIEnv* env, [[maybe_unused]] jclass clazz, jstring uri)
{
	auto gamePath = JNIUtils::toString(env, uri);
	auto& gamePaths = g_config.data().game_paths;
	std::erase_if(gamePaths, [&](auto path) { return path == gamePath; });
}

extern "C" [[maybe_unused]] JNIEXPORT jobject JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getGamesPaths(JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return JNIUtils::createJavaStringArrayList(env, g_config.data().game_paths);
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getAsyncShaderCompile([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().async_compile;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setAsyncShaderCompile([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean enabled)
{
	g_config.data().async_compile = enabled;
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getVsyncMode([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().vsync;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setVsyncMode([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint vsync_mode)
{
	g_config.data().vsync = vsync_mode;
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getAccurateBarriers([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().vk_accurate_barriers;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setUpscalingFilter([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint upscaling_filter)
{
	g_config.data().upscale_filter = upscaling_filter;
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getUpscalingFilter([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().upscale_filter;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setDownscalingFilter([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint downscaling_filter)
{
	g_config.data().downscale_filter = downscaling_filter;
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getDownscalingFilter([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().downscale_filter;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setFullscreenScaling([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint fullscreen_scaling)
{
	g_config.data().fullscreen_scaling = fullscreen_scaling;
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getFullscreenScaling([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().fullscreen_scaling;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setAccurateBarriers([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean enabled)
{
	g_config.data().vk_accurate_barriers = enabled;
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getAudioDeviceEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean tv)
{
	const auto& device = tv ? g_config.data().tv_device : g_config.data().pad_device;
	return !device.empty();
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setAudioDeviceEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean enabled, jboolean tv)
{
	auto& device = tv ? g_config.data().tv_device : g_config.data().pad_device;
	if (enabled)
		device = L"Default";
	else
		device.clear();
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getAudioDeviceChannels([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean tv)
{
	const auto& deviceChannels = tv ? g_config.data().tv_channels : g_config.data().pad_channels;
	return deviceChannels;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setAudioDeviceChannels([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint channels, jboolean tv)
{
	auto& deviceChannels = tv ? g_config.data().tv_channels : g_config.data().pad_channels;
	deviceChannels = static_cast<AudioChannels>(channels);
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getAudioDeviceVolume([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean tv)
{
	const auto& deviceVolume = tv ? g_config.data().tv_volume : g_config.data().pad_volume;
	return deviceVolume;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setAudioDeviceVolume([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint volume, jboolean tv)
{
	auto& deviceVolume = tv ? g_config.data().tv_volume : g_config.data().pad_volume;
	deviceVolume = volume;
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getAudioLatency([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return g_config.data().audio_delay * 12;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setAudioLatency([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint latency)
{
	g_config.data().audio_delay = latency / 12;
	IAudioAPI::SetAudioDelay(g_config.data().audio_delay);
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getConsoleLanguage([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return static_cast<jint>(g_config.data().console_language.GetValue());
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setConsoleLanguage([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint console_language)
{
	g_config.data().console_language = static_cast<CafeConsoleLanguage>(console_language);
}

extern "C" [[maybe_unused]] JNIEXPORT jstring JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_getCustomDriverPath(JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	std::string customDriverPath = g_config.data().custom_driver_path;
	if (customDriverPath.empty())
		return nullptr;
	return JNIUtils::toJString(env, customDriverPath);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_setCustomDriverPath(JNIEnv* env, [[maybe_unused]] jclass clazz, jstring custom_driver_path)
{
	g_config.data().custom_driver_path = JNIUtils::toString(env, custom_driver_path);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeSettings_saveSettings([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	g_config.Save();
}