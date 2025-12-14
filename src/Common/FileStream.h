#pragma once
#include "Common/precompiled.h"

#ifdef _WIN32
#include "Common/windows/FileStream_win32.h"
#elif __ANDROID__
#include "Common/android/FileStream_android.h"
using FileStream = FileStreamAndroid;
#else
#include "Common/unix/FileStream_unix.h"
using FileStream = FileStreamUnix;
#endif
