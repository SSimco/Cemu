#pragma once

namespace FilesystemAndroid
{
class FilesystemCallbacks
{
   public:
    virtual int OpenContentUri(const fs::path &uri) = 0;
    virtual std::vector<fs::path> ListFiles(const fs::path &uri) = 0;
    virtual bool IsDirectory(const fs::path &uri) = 0;
    virtual bool IsFile(const fs::path &uri) = 0;
    virtual bool Exists(const fs::path &uri) = 0;
};

void SetFilesystemCallbacks(const std::shared_ptr<FilesystemCallbacks> &filesystemCallbacks);

int OpenContentUri(const fs::path &uri);

std::vector<fs::path> ListFiles(const fs::path &uri);

bool IsDirectory(const fs::path &uri);

bool IsFile(const fs::path &uri);

bool Exists(const fs::path& uri);

bool IsContentUri(const std::string &uri);

}  // namespace FilesystemAndroid
