#pragma once

#include "JNIUtils.h"
#include "Common/unix/FilesystemAndroid.h"

class AndroidFilesystemCallbacks : public FilesystemAndroid::FilesystemCallbacks {
	jmethodID m_openContentUriMid;
	jmethodID m_listFilesMid;
	jmethodID m_isDirectoryMid;
	jmethodID m_isFileMid;
	jmethodID m_existsMid;
	JNIUtils::Scopedjclass m_fileUtilClass;

	bool callBooleanFunction(const std::filesystem::path& uri, jmethodID methodId)
	{
		bool result = false;
		JNIUtils::fiberSafeJNICall([&](JNIEnv* env) {
			jstring uriString = JNIUtils::toJString(env, uri);
			result = env->CallStaticBooleanMethod(*m_fileUtilClass, methodId, uriString);
			env->DeleteLocalRef(uriString);
		});
		return result;
	}

  public:
	AndroidFilesystemCallbacks()
	{
		JNIUtils::ScopedJNIENV env;
		m_fileUtilClass = JNIUtils::Scopedjclass("info/cemu/cemu/nativeinterface/NativeFilesKt");
		m_openContentUriMid = env->GetStaticMethodID(*m_fileUtilClass, "openContentUri", "(Ljava/lang/String;)I");
		m_listFilesMid = env->GetStaticMethodID(*m_fileUtilClass, "listFiles", "(Ljava/lang/String;)[Ljava/lang/String;");
		m_isDirectoryMid = env->GetStaticMethodID(*m_fileUtilClass, "isDirectory", "(Ljava/lang/String;)Z");
		m_isFileMid = env->GetStaticMethodID(*m_fileUtilClass, "isFile", "(Ljava/lang/String;)Z");
		m_existsMid = env->GetStaticMethodID(*m_fileUtilClass, "exists", "(Ljava/lang/String;)Z");
	}

	int openContentUri(const std::filesystem::path& uri) override
	{
		int fd = -1;
		JNIUtils::fiberSafeJNICall([&](JNIEnv* env) {
			jstring uriString = JNIUtils::toJString(env, uri);
			fd = env->CallStaticIntMethod(*m_fileUtilClass, m_openContentUriMid, uriString);
			env->DeleteLocalRef(uriString);
		});
		return fd;
	}

	std::vector<std::filesystem::path> listFiles(const std::filesystem::path& uri) override
	{
		std::vector<std::filesystem::path> paths;
		JNIUtils::fiberSafeJNICall([&](JNIEnv* env) {
			jstring uriString = JNIUtils::toJString(env, uri);
			jobjectArray pathsObjArray = static_cast<jobjectArray>(env->CallStaticObjectMethod(*m_fileUtilClass, m_listFilesMid, uriString));
			env->DeleteLocalRef(uriString);
			jsize arrayLength = env->GetArrayLength(pathsObjArray);
			paths.reserve(arrayLength);
			for (jsize i = 0; i < arrayLength; i++)
			{
				jstring pathStr = static_cast<jstring>(env->GetObjectArrayElement(pathsObjArray, i));
				paths.push_back(JNIUtils::toString(env, pathStr));
				env->DeleteLocalRef(pathStr);
			}
			env->DeleteLocalRef(pathsObjArray);
		});
		return paths;
	}

	bool isDirectory(const std::filesystem::path& uri) override
	{
		return callBooleanFunction(uri, m_isDirectoryMid);
	}

	bool isFile(const std::filesystem::path& uri) override
	{
		return callBooleanFunction(uri, m_isFileMid);
	}

	bool exists(const std::filesystem::path& uri) override
	{
		return callBooleanFunction(uri, m_existsMid);
	}
};