#include "JNIUtils.h"

namespace NativeLocalization
{
	std::unordered_map<std::string_view, std::string> g_messages;

	std::string Translate(std::string_view msgId)
	{
		if (auto message = g_messages.find(msgId); message != g_messages.end())
		{
			return message->second;
		}

		return std::string{msgId};
	}
} // namespace NativeLocalization

extern "C" JNIEXPORT [[maybe_unused]] void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeLocalization_setTranslations(JNIEnv* env, [[maybe_unused]] jclass clazz, jobject translations)
{
    NativeLocalization::g_messages.clear();

	jclass mapClass = env->GetObjectClass(translations);
	jmethodID keySetMethodId = env->GetMethodID(mapClass, "keySet", "()Ljava/util/Set;");
	jmethodID getMethodId = env->GetMethodID(mapClass, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
	jobject keySet = env->CallObjectMethod(translations, keySetMethodId);
	jclass setClass = env->GetObjectClass(keySet);
	jmethodID toArrayMethodId = env->GetMethodID(setClass, "toArray", "()[Ljava/lang/Object;");
	auto keyArray = static_cast<jobjectArray>(env->CallObjectMethod(keySet, toArrayMethodId));
	jint size = env->GetArrayLength(keyArray);

	for (jint i = 0; i < size; i++)
	{
		auto keyJava = static_cast<jstring>(env->GetObjectArrayElement(keyArray, i));
		std::string key = JNIUtils::toString(env, keyJava);
		auto translationJava = static_cast<jstring>(env->CallObjectMethod(translations, getMethodId, keyJava));
		std::string translation = JNIUtils::toString(env, translationJava);
		NativeLocalization::g_messages[key] = translation;
	}

    SetTranslationCallback(NativeLocalization::Translate);
}