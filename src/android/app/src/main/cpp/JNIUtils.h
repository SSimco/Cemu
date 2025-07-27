#pragma once

#include <android/native_window_jni.h>
#include <jni.h>

namespace JNIUtils
{
	extern JavaVM* g_jvm;

	inline std::string toString(JNIEnv* env, jstring jstr)
	{
		if (jstr == nullptr)
			return {};
		const char* c_str = env->GetStringUTFChars(jstr, nullptr);
		std::string str(c_str);
		env->ReleaseStringUTFChars(jstr, c_str);
		return str;
	}

	inline jstring toJString(JNIEnv* env, const std::string& str)
	{
		return env->NewStringUTF(str.c_str());
	}

	inline jstring toJString(JNIEnv* env, std::string_view str)
	{
		return toJString(env, std::string(str));
	}

	inline jstring toJString(JNIEnv* env, std::wstring_view str)
	{
		return toJString(env, boost::nowide::narrow(str));
	}

	jobject createJavaStringArrayList(JNIEnv* env, const std::vector<std::string>& stringList);

	jobject createJavaStringArrayList(JNIEnv* env, const std::vector<std::wstring>& stringList);

	void handleNativeException(JNIEnv* env, const std::function<void()>& fn);

	class ScopedJNIENV
	{
	  public:
		ScopedJNIENV()
		{
			jint result = g_jvm->GetEnv((void**)&m_env, JNI_VERSION_1_6);

			if (result != JNI_EDETACHED)
				return;

			JavaVMAttachArgs args;
			args.version = JNI_VERSION_1_6;
			args.name = nullptr;
			args.group = nullptr;
			result = g_jvm->AttachCurrentThread(&m_env, &args);
			if (result == JNI_OK)
				m_threadWasAttached = true;
		}

		JNIEnv*& operator*()
		{
			return m_env;
		}

		JNIEnv* operator->()
		{
			return m_env;
		}

		operator JNIEnv*() const
		{
			return m_env;
		}

		~ScopedJNIENV()
		{
			if (m_threadWasAttached)
				g_jvm->DetachCurrentThread();
		}

	  private:
		JNIEnv* m_env = nullptr;
		bool m_threadWasAttached = false;
	};

	class Scopedjobject
	{
	  public:
		Scopedjobject() = default;

		Scopedjobject(Scopedjobject&& other) noexcept
		{
			this->m_jobject = other.m_jobject;
			other.m_jobject = nullptr;
		}
		void deleteRef()
		{
			if (m_jobject)
			{
				ScopedJNIENV()->DeleteGlobalRef(m_jobject);
				m_jobject = nullptr;
			}
		}
		Scopedjobject& operator=(Scopedjobject&& other) noexcept
		{
			if (this != &other)
			{
				deleteRef();
				m_jobject = other.m_jobject;
				other.m_jobject = nullptr;
			}
			return *this;
		}
		const jobject& operator*() const
		{
			return m_jobject;
		}

		explicit Scopedjobject(jobject obj)
		{
			if (obj)
				m_jobject = ScopedJNIENV()->NewGlobalRef(obj);
		}

		~Scopedjobject()
		{
			deleteRef();
		}

		bool isValid() const
		{
			return m_jobject;
		}

	  private:
		jobject m_jobject = nullptr;
	};

	class Scopedjclass
	{
	  public:
		Scopedjclass() = default;

		Scopedjclass(Scopedjclass&& other) noexcept
		{
			this->m_jclass = other.m_jclass;
			other.m_jclass = nullptr;
		}

		explicit Scopedjclass(jclass javaClass)
		{
			if (javaClass)
				m_jclass = static_cast<jclass>(ScopedJNIENV()->NewGlobalRef(javaClass));
		}

		Scopedjclass& operator=(Scopedjclass&& other) noexcept
		{
			if (this != &other)
			{
				if (m_jclass)
					ScopedJNIENV()->DeleteGlobalRef(m_jclass);
				m_jclass = other.m_jclass;
				other.m_jclass = nullptr;
			}
			return *this;
		}

		explicit Scopedjclass(const std::string& className)
		{
			ScopedJNIENV scopedEnv;
			jclass tempObj = scopedEnv->FindClass(className.c_str());
			m_jclass = static_cast<jclass>(scopedEnv->NewGlobalRef(tempObj));
			scopedEnv->DeleteLocalRef(tempObj);
		}

		~Scopedjclass()
		{
			if (m_jclass)
				ScopedJNIENV()->DeleteGlobalRef(m_jclass);
		}

		bool isValid() const
		{
			return m_jclass != nullptr;
		}

		const jclass& operator*() const
		{
			return m_jclass;
		}

	  private:
		jclass m_jclass = nullptr;
	};

	Scopedjobject getEnumValue(JNIEnv* env, const std::string& enumClassName, const std::string& enumName);
	jobject createArrayList(JNIEnv* env, const std::vector<jobject>& objects);
	jobject createJavaLongArrayList(JNIEnv* env, const std::vector<uint64_t>& values);

	template<typename... TArgs>
	jobject newObject(JNIEnv* env, const std::string& className, const std::string& ctrSig = "()V", TArgs&&... args)
	{
		jclass javaClass = env->FindClass(className.c_str());
		jmethodID ctrId = env->GetMethodID(javaClass, "<init>", ctrSig.c_str());
		jobject obj = env->NewObject(javaClass, ctrId, std::forward<TArgs>(args)...);
		env->DeleteLocalRef(javaClass);
		return obj;
	}

	inline void fiberSafeJNICall(const std::function<void(JNIEnv*)>& func)
	{
		std::thread([&]() {
			ScopedJNIENV env;
			func(*env);
		}).join();
	}
} // namespace JNIUtils
