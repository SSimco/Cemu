#include "JNIUtils.h"
#include "input/ControllerFactory.h"
#include "input/InputManager.h"
#include "input/api/Android/AndroidController.h"
#include "input/api/Android/AndroidControllerProvider.h"
#include "AndroidEmulatedController.h"

namespace NativeInput
{
	WiiUMotionHandler s_wiiUMotionHandler{};
	long s_lastMotionTimestamp = 0;

	void onTouchEvent(sint32 x, sint32 y, bool isTV, std::optional<bool> status = {})
	{
		auto& instance = InputManager::instance();
		auto& touchInfo = isTV ? instance.m_main_mouse : instance.m_pad_mouse;
		std::scoped_lock lock(touchInfo.m_mutex);
		touchInfo.position = {x, y};
		if (status.has_value())
			touchInfo.left_down = touchInfo.left_down_toggle = status.value();
	}
} // namespace NativeInput

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_onNativeKey(JNIEnv* env, [[maybe_unused]] jclass clazz, jstring device_descriptor, jstring device_name, jint key, jboolean is_pressed)
{
	auto deviceDescriptor = JNIUtils::toString(env, device_descriptor);
	auto deviceName = JNIUtils::toString(env, device_name);
	auto apiProvider = InputManager::instance().get_api_provider(InputAPI::Android);
	auto androidControllerProvider = dynamic_cast<AndroidControllerProvider*>(apiProvider.get());
	androidControllerProvider->on_key_event(deviceDescriptor, deviceName, key, is_pressed);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_onNativeAxis(JNIEnv* env, [[maybe_unused]] jclass clazz, jstring device_descriptor, jstring device_name, jint axis, jfloat value)
{
	auto deviceDescriptor = JNIUtils::toString(env, device_descriptor);
	auto deviceName = JNIUtils::toString(env, device_name);
	auto apiProvider = InputManager::instance().get_api_provider(InputAPI::Android);
	auto androidControllerProvider = dynamic_cast<AndroidControllerProvider*>(apiProvider.get());
	androidControllerProvider->on_axis_event(deviceDescriptor, deviceName, axis, value);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_setControllerType([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint index, jint emulated_controller_type)
{
	auto type = static_cast<EmulatedController::Type>(emulated_controller_type);
	auto& androidEmulatedController = AndroidEmulatedController::getAndroidEmulatedController(index);
	if (EmulatedController::Type::VPAD <= type && type < EmulatedController::Type::MAX)
		androidEmulatedController.setType(type);
	else
		androidEmulatedController.setDisabled();
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_getControllerType([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint index)
{
	auto emulatedController = AndroidEmulatedController::getAndroidEmulatedController(index).getEmulatedController();
	if (emulatedController)
		return emulatedController->type();
	throw std::runtime_error(fmt::format("can't get type for emulated controller {}", index));
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_getWPADControllersCount([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	int wpadCount = 0;
	for (size_t i = 0; i < InputManager::kMaxController; i++)
	{
		auto emulatedController = AndroidEmulatedController::getAndroidEmulatedController(i).getEmulatedController();
		if (!emulatedController)
			continue;
		if (emulatedController->type() != EmulatedController::Type::VPAD)
			++wpadCount;
	}
	return wpadCount;
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_getVPADControllersCount([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	int vpadCount = 0;
	for (size_t i = 0; i < InputManager::kMaxController; i++)
	{
		auto emulatedController = AndroidEmulatedController::getAndroidEmulatedController(i).getEmulatedController();
		if (!emulatedController)
			continue;
		if (emulatedController->type() == EmulatedController::Type::VPAD)
			++vpadCount;
	}
	return vpadCount;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_setVPADScreenToggle([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint index, jboolean enabled)
{
	auto emulatedController = AndroidEmulatedController::getAndroidEmulatedController(index).getEmulatedController();
	if (emulatedController == nullptr || emulatedController->type() != EmulatedController::Type::VPAD)
		throw std::runtime_error(fmt::format("Invalid controller type for controller {}, expected VPAD", index));
	static_cast<VPADController*>(emulatedController.get())->set_screen_toggle(enabled);
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_getVPADScreenToggle([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint index)
{
	auto emulatedController = AndroidEmulatedController::getAndroidEmulatedController(index).getEmulatedController();
	if (emulatedController == nullptr || emulatedController->type() != EmulatedController::Type::VPAD)
		throw std::runtime_error(fmt::format("Invalid controller type for controller {}, expected VPAD", index));
	return static_cast<VPADController*>(emulatedController.get())->is_screen_active_toggle();
}

extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_isControllerDisabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint index)
{
	return AndroidEmulatedController::getAndroidEmulatedController(index).getEmulatedController() == nullptr;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_setControllerMapping(JNIEnv* env, [[maybe_unused]] jclass clazz, jstring device_descriptor, jstring device_name, jint index, jint mapping_id, jint button_id)
{
	auto deviceName = JNIUtils::toString(env, device_name);
	auto deviceDescriptor = JNIUtils::toString(env, device_descriptor);
	auto apiProvider = InputManager::instance().get_api_provider(InputAPI::Android);
	auto controller = ControllerFactory::CreateController(InputAPI::Android, deviceDescriptor, deviceName);
	AndroidEmulatedController::getAndroidEmulatedController(index).setMapping(mapping_id, controller, button_id);
}

extern "C" [[maybe_unused]] JNIEXPORT jstring JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_getControllerMapping(JNIEnv* env, [[maybe_unused]] jclass clazz, jint index, jint mapping_id)
{
	auto mapping = AndroidEmulatedController::getAndroidEmulatedController(index).getMapping(mapping_id);
	return JNIUtils::toJString(env, mapping.value_or(""));
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_clearControllerMapping([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint index, jint mapping_id)
{
	AndroidEmulatedController::getAndroidEmulatedController(index).clearMapping(mapping_id);
}

extern "C" [[maybe_unused]] JNIEXPORT jobject JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_getControllerMappings(JNIEnv* env, [[maybe_unused]] jclass clazz, jint index)
{
	jclass hashMapClass = env->FindClass("java/util/HashMap");
	jmethodID hashMapConstructor = env->GetMethodID(hashMapClass, "<init>", "()V");
	jmethodID hashMapPut = env->GetMethodID(hashMapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
	jclass integerClass = env->FindClass("java/lang/Integer");
	jmethodID integerConstructor = env->GetMethodID(integerClass, "<init>", "(I)V");
	jobject hashMapObj = env->NewObject(hashMapClass, hashMapConstructor);
	auto mappings = AndroidEmulatedController::getAndroidEmulatedController(index).getMappings();
	for (const auto& pair : mappings)
	{
		jint key = pair.first;
		jstring buttonName = JNIUtils::toJString(env, pair.second);
		jobject mappingId = env->NewObject(integerClass, integerConstructor, key);
		env->CallObjectMethod(hashMapObj, hashMapPut, mappingId, buttonName);
	}
	return hashMapObj;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_onTouchDown([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint x, jint y, jboolean isTV)
{
	NativeInput::onTouchEvent(x, y, isTV, true);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_onTouchUp([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint x, jint y, jboolean isTV)
{
	NativeInput::onTouchEvent(x, y, isTV, false);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_onTouchMove([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint x, jint y, jboolean isTV)
{
	NativeInput::onTouchEvent(x, y, isTV);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_onMotion([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jlong timestamp, jfloat gyroX, jfloat gyroY, jfloat gyroZ, jfloat accelX, jfloat accelY, jfloat accelZ)
{
	static constexpr float METERS_PER_SECOND_SQ_TO_G = 1.f / 9.81f;
	float deltaTime = (timestamp - NativeInput::s_lastMotionTimestamp) * 1e-9f;
	NativeInput::s_wiiUMotionHandler.processMotionSample(deltaTime, gyroX, gyroY, gyroZ, accelX * METERS_PER_SECOND_SQ_TO_G, accelY * METERS_PER_SECOND_SQ_TO_G, accelZ * METERS_PER_SECOND_SQ_TO_G);
	NativeInput::s_lastMotionTimestamp = timestamp;
	auto& deviceMotion = InputManager::instance().m_device_motion;
	std::scoped_lock lock{deviceMotion.m_mutex};
	deviceMotion.m_motion_sample = NativeInput::s_wiiUMotionHandler.getMotionSample();
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_setMotionEnabled([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean motionEnabled)
{
	auto& deviceMotion = InputManager::instance().m_device_motion;
	std::scoped_lock lock{deviceMotion.m_mutex};
	deviceMotion.m_device_motion_enabled = motionEnabled;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_onOverlayButton([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint controllerIndex, jint mappingId, jboolean state)
{
	AndroidEmulatedController::getAndroidEmulatedController(controllerIndex).setButtonValue(mappingId, state);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeInput_onOverlayAxis([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint controllerIndex, jint mappingId, jfloat value)
{
	AndroidEmulatedController::getAndroidEmulatedController(controllerIndex).setAxisValue(mappingId, value);
}