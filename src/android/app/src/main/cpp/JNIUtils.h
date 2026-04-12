#pragma once

#include <jni.h>

namespace JNIUtils
{
	void SetJavaVM(JavaVM* jvm);

	inline std::string FromJString(JNIEnv* env, jstring jstr)
	{
		if (jstr == nullptr)
			return {};
		const char* c_str = env->GetStringUTFChars(jstr, nullptr);
		std::string str(c_str);
		env->ReleaseStringUTFChars(jstr, c_str);
		return str;
	}

	inline jstring ToJString(JNIEnv* env, const std::string& str)
	{
		return env->NewStringUTF(str.c_str());
	}

	inline jstring ToJString(JNIEnv* env, std::string_view str)
	{
		return ToJString(env, std::string(str));
	}

	inline jstring ToJString(JNIEnv* env, std::wstring_view str)
	{
		return ToJString(env, boost::nowide::narrow(str));
	}

	jobject CreateJavaStringArrayList(JNIEnv* env, const std::vector<std::string>& stringList);

	jobject CreateJavaStringArrayList(JNIEnv* env, const std::vector<std::wstring>& stringList);

	inline void HandleNativeException(JNIEnv* env, std::invocable auto fn)
	{
		try
		{
			fn();
		} catch (const std::exception& exception)
		{
			jclass exceptionClass = env->FindClass("info/cemu/cemu/nativeinterface/NativeException");
			env->ThrowNew(exceptionClass, exception.what());
		} catch (...)
		{
			jclass exceptionClass = env->FindClass("info/cemu/cemu/nativeinterface/NativeException");
			env->ThrowNew(exceptionClass, "Unknown native exception");
		}
	}

	JNIEnv* GetEnv();

	class Scopedjobject
	{
	  public:
		Scopedjobject() = default;

		Scopedjobject(Scopedjobject&& other) noexcept;

		void DeleteReference();

		Scopedjobject& operator=(Scopedjobject&& other) noexcept;

		jobject operator*() const;

		explicit Scopedjobject(jobject obj);

		~Scopedjobject();

	  private:
		jobject m_jobject = nullptr;
	};

	class Scopedjclass
	{
	  public:
		Scopedjclass() = default;

		Scopedjclass(Scopedjclass&& other) noexcept;

		explicit Scopedjclass(jclass javaClass);

		Scopedjclass& operator=(Scopedjclass&& other) noexcept;

		explicit Scopedjclass(const char* className);

		~Scopedjclass();

		jclass operator*() const;

	  private:
		jclass m_jclass = nullptr;
	};

	Scopedjobject GetEnumValue(JNIEnv* env, const std::string& enumClassName, const std::string& enumName);

	jobject CreateArrayList(JNIEnv* env, const std::vector<jobject>& objects);

	jobject CreateJavaLongArrayList(JNIEnv* env, const std::vector<uint64_t>& values);

	template<typename... TArgs>
	jobject NewObject(JNIEnv* env, const char* className, const std::string& ctrSig = "()V", TArgs&&... args)
	{
		jclass javaClass = env->FindClass(className);
		jmethodID ctrId = env->GetMethodID(javaClass, "<init>", ctrSig.c_str());
		jobject obj = env->NewObject(javaClass, ctrId, std::forward<TArgs>(args)...);
		env->DeleteLocalRef(javaClass);
		return obj;
	}

	inline void FiberSafeJNICall(std::invocable<JNIEnv*> auto func)
	{
		std::jthread([&]() {
			func(GetEnv());
		});
	}
} // namespace JNIUtils
