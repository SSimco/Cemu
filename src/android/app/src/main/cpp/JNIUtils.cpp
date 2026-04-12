#include "JNIUtils.h"

static JavaVM* s_jvm = nullptr;

namespace JNIUtils
{
	void SetJavaVM(JavaVM* jvm)
	{
		s_jvm = jvm;
	}

	Scopedjobject::Scopedjobject(Scopedjobject&& other) noexcept
	{
		this->m_jobject = other.m_jobject;
		other.m_jobject = nullptr;
	}

	void Scopedjobject::DeleteReference()
	{
		if (m_jobject)
		{
			GetEnv()->DeleteGlobalRef(m_jobject);
			m_jobject = nullptr;
		}
	}

	Scopedjobject& Scopedjobject::operator=(Scopedjobject&& other) noexcept
	{
		if (this != &other)
		{
			DeleteReference();
			m_jobject = other.m_jobject;
			other.m_jobject = nullptr;
		}
		return *this;
	}

	jobject Scopedjobject::operator*() const
	{
		return m_jobject;
	}

	Scopedjobject::Scopedjobject(jobject obj)
	{
		if (obj)
			m_jobject = GetEnv()->NewGlobalRef(obj);
	}

	Scopedjobject::~Scopedjobject()
	{
		DeleteReference();
	}

	Scopedjclass::Scopedjclass(Scopedjclass&& other) noexcept
	{
		this->m_jclass = other.m_jclass;
		other.m_jclass = nullptr;
	}

	Scopedjclass::Scopedjclass(jclass javaClass)
	{
		if (javaClass)
			m_jclass = static_cast<jclass>(GetEnv()->NewGlobalRef(javaClass));
	}

	Scopedjclass& Scopedjclass::operator=(Scopedjclass&& other) noexcept
	{
		if (this != &other)
		{
			if (m_jclass)
				GetEnv()->DeleteGlobalRef(m_jclass);
			m_jclass = other.m_jclass;
			other.m_jclass = nullptr;
		}
		return *this;
	}

	Scopedjclass::Scopedjclass(const char* className)
	{
		JNIEnv* env = GetEnv();
		jclass tempObj = env->FindClass(className);
		m_jclass = static_cast<jclass>(env->NewGlobalRef(tempObj));
		env->DeleteLocalRef(tempObj);
	}

	Scopedjclass::~Scopedjclass()
	{
		if (m_jclass)
			GetEnv()->DeleteGlobalRef(m_jclass);
	}

	jclass Scopedjclass::operator*() const
	{
		return m_jclass;
	}

	jobject CreateJavaStringArrayList(JNIEnv* env, const std::vector<std::string>& strings)
	{
		jclass clsArrayList = env->FindClass("java/util/ArrayList");
		jmethodID arrayListConstructor = env->GetMethodID(clsArrayList, "<init>", "()V");
		jobject arrayListObject = env->NewObject(clsArrayList, arrayListConstructor);
		jmethodID addMethod = env->GetMethodID(clsArrayList, "add", "(Ljava/lang/Object;)Z");
		env->DeleteLocalRef(clsArrayList);

		for (const auto& string : strings)
		{
			jstring element = env->NewStringUTF(string.c_str());
			env->CallBooleanMethod(arrayListObject, addMethod, element);
			env->DeleteLocalRef(element);
		}
		return arrayListObject;
	}

	Scopedjobject GetEnumValue(JNIEnv* env, const std::string& enumClassName, const std::string& enumName)
	{
		jclass enumClass = env->FindClass(enumClassName.c_str());
		jfieldID fieldID = env->GetStaticFieldID(enumClass, enumName.c_str(), ("L" + enumClassName + ";").c_str());
		jobject enumValue = env->GetStaticObjectField(enumClass, fieldID);
		env->DeleteLocalRef(enumClass);
		Scopedjobject enumObj = Scopedjobject(enumValue);
		env->DeleteLocalRef(enumValue);
		return enumObj;
	}

	jobject CreateArrayList(JNIEnv* env, const std::vector<jobject>& objects)
	{
		static Scopedjclass listClass = Scopedjclass("java/util/ArrayList");
		static jmethodID listConstructor = env->GetMethodID(*listClass, "<init>", "()V");
		static jmethodID listAdd = env->GetMethodID(*listClass, "add", "(Ljava/lang/Object;)Z");

		jobject arrayList = env->NewObject(*listClass, listConstructor);
		for (auto&& obj : objects)
			env->CallBooleanMethod(arrayList, listAdd, obj);
		return arrayList;
	}

	jobject CreateJavaLongArrayList(JNIEnv* env, const std::vector<uint64_t>& values)
	{
		jclass longClass = env->FindClass("java/lang/Long");
		jmethodID valueOf = env->GetStaticMethodID(longClass, "valueOf", "(J)Ljava/lang/Long;");
		std::vector<jobject> valuesJava;
		valuesJava.reserve(values.size());
		for (auto&& value : values)
			valuesJava.push_back(env->CallStaticObjectMethod(longClass, valueOf, value));
		env->DeleteLocalRef(longClass);
		return CreateArrayList(env, valuesJava);
	}

	jobject CreateJavaStringArrayList(JNIEnv* env, const std::vector<std::wstring>& strings)
	{
		jclass arrayListClass = env->FindClass("java/util/ArrayList");
		jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
		jobject arrayListObject = env->NewObject(arrayListClass, arrayListConstructor);
		jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
		for (const auto& string : strings)
		{
			jstring element = env->NewString((jchar*)string.c_str(), string.length());
			env->CallBooleanMethod(arrayListObject, addMethod, element);
			env->DeleteLocalRef(element);
		}
		return arrayListObject;
	}

	JNIEnv* GetEnv()
	{
		thread_local static struct OwnedEnv
		{
			JNIEnv* env;
			jint result;

			OwnedEnv()
			{
				result = s_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);

				if (result == JNI_EDETACHED)
					s_jvm->AttachCurrentThread(&env, nullptr);
			}

			~OwnedEnv()
			{
				if (result == JNI_EDETACHED)
					s_jvm->DetachCurrentThread();
			}
		} owned;

		return owned.env;
	}
}; // namespace JNIUtils
