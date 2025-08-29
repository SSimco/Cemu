#pragma once

#include <zarchive/zarchivereader.h>

#if __ANDROID__
#include "Common/android/FilesystemAndroid.h"
#include "Common/android/FdStream.h"
#endif // __ANDROID__

namespace ZArchiveHelpers
{
	inline ZArchiveReader* OpenReader(const fs::path& path)
	{
#if __ANDROID__
		if (FilesystemAndroid::IsContentUri(path))
		{
			int fd = FilesystemAndroid::OpenContentUri(path);

			if (fd == -1)
				return nullptr;

			return ZArchiveReader::OpenFromStream(std::make_unique<FdStream>(fd));
		}
#endif // __ANDROID__

		return ZArchiveReader::OpenFromFile(path);
	}
} // namespace ZArchiveHelpers
