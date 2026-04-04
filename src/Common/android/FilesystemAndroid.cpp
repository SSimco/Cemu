#include "FilesystemAndroid.h"

#include <boost/iostreams/device/file_descriptor.hpp>
#include <boost/iostreams/stream_buffer.hpp>

using namespace boost::iostreams;

namespace FilesystemAndroid
{
std::shared_ptr<FilesystemCallbacks> g_filesystemCallbacks = nullptr;

void SetFilesystemCallbacks(std::shared_ptr<FilesystemCallbacks> filesystemCallbacks)
{
    g_filesystemCallbacks = std::move(filesystemCallbacks);
}

int OpenContentUri(const fs::path &uri)
{
    if (g_filesystemCallbacks)
        return g_filesystemCallbacks->OpenContentUri(uri);
    return -1;
}

std::vector<fs::path> ListFiles(const fs::path &uri)
{
    if (g_filesystemCallbacks)
        return g_filesystemCallbacks->ListFiles(uri);
    return {};
}

bool IsDirectory(const fs::path &uri)
{
    if (g_filesystemCallbacks)
        return g_filesystemCallbacks->IsDirectory(uri);
    return false;
}

bool IsFile(const fs::path &uri)
{
    if (g_filesystemCallbacks)
        return g_filesystemCallbacks->IsFile(uri);
    return false;
}

bool Exists(const fs::path &uri)
{
    if (g_filesystemCallbacks)
        return g_filesystemCallbacks->Exists(uri);
    return false;
}

bool IsContentUri(std::string_view uri)
{
    static constexpr auto content = "content://";
    return uri.starts_with(content);
}

}  // namespace FilesystemAndroid