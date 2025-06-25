#include "WuaConverter.h"

WuaConverter::WuaConverter(const TitleInfo& titleInfo_base, const TitleInfo& titleInfo_update, const TitleInfo& titleInfo_aoc)
	: m_titleInfo_base{titleInfo_base},
	  m_titleInfo_update{titleInfo_update},
	  m_titleInfo_aoc{titleInfo_aoc}
{
}
WuaConverter::~WuaConverter()
{
	m_writerContext.cancelled = true;
	if (m_workerThread.joinable())
		m_workerThread.join();
}

void WuaConverter::startConversion(int fd, std::unique_ptr<CompressTitleCallbacks>&& callbacks)
{
	m_workerThread = std::thread([callbacks(std::move(callbacks)), fd, this]() {
	  if (fd == -1)
	  {
		  callbacks->onError();
		  return;
	  }
	  stdx::scope_exit fdCleanup([fd](){ close(fd); });

	  if (m_started)
		  return;

	  m_started = true;

	  std::vector<TitleInfo*> titlesToConvert;
	  if (m_titleInfo_base.IsValid())
		  titlesToConvert.emplace_back(&m_titleInfo_base);
	  if (m_titleInfo_update.IsValid())
		  titlesToConvert.emplace_back(&m_titleInfo_update);
	  if (m_titleInfo_aoc.IsValid())
		  titlesToConvert.emplace_back(&m_titleInfo_aoc);

	  if (titlesToConvert.empty())
	  {
		  callbacks->onError();
		  return;
	  }

	  // mount and store
	  m_writerContext.isValid = true;
	  m_writerContext.fd = fd;
	  m_writerContext.zaWriter = std::make_unique<ZArchiveWriter>(&ZArchiveWriterContext::NewOutputFile, &ZArchiveWriterContext::WriteOutputData, &m_writerContext);
	  if (!m_writerContext.isValid)
	  {
		  callbacks->onError();
		  return;
	  }

	  bool result = m_writerContext.AddTitles(titlesToConvert.data(), titlesToConvert.size());

	  if (m_writerContext.cancelled)
		  return;

	  if (!result)
	  {
		  callbacks->onError();
		  return;
	  }

	  m_writerContext.zaWriter->Finalize();

	  // verify the created WUA file
	  boost::iostreams::stream_buffer<boost::iostreams::file_descriptor_source> stream(fd, boost::iostreams::never_close_handle);
	  ZArchiveReader* zreader = ZArchiveReader::OpenFromStream(std::make_unique<std::istream>(&stream));
	  if (!zreader)
	  {
		  callbacks->onError();
		  return;
	  }
	  // todo - do a quick verification here
	  delete zreader;

	  CafeTitleList::Refresh();

	  callbacks->onFinished();
	});
}

uint64 WuaConverter::getTransferredInputBytes() const
{
	return m_writerContext.transferredInputBytes.load(std::memory_order_relaxed);
}

std::string WuaConverter::getCompressedFileName()
{
	CafeConsoleLanguage languageId = CafeConsoleLanguage::EN; // todo - use user's locale
	std::string shortName;
	if (m_titleInfo_base.IsValid())
		shortName = m_titleInfo_base.GetMetaInfo()->GetShortName(languageId);
	else if (m_titleInfo_update.IsValid())
		shortName = m_titleInfo_update.GetMetaInfo()->GetShortName(languageId);
	else if (m_titleInfo_aoc.IsValid())
		shortName = m_titleInfo_aoc.GetMetaInfo()->GetShortName(languageId);

	if (!shortName.empty())
	{
		boost::replace_all(shortName, ":", "");
	}

	// get the short name, which we will use as a suggested default file name
	std::string defaultFileName = std::move(shortName);
	boost::replace_all(defaultFileName, "/", "");
	boost::replace_all(defaultFileName, "\\", "");

	CafeConsoleRegion region = CafeConsoleRegion::Auto;
	if (m_titleInfo_base.IsValid() && m_titleInfo_base.HasValidXmlInfo())
		region = m_titleInfo_base.GetMetaInfo()->GetRegion();
	else if (m_titleInfo_update.IsValid() && m_titleInfo_update.HasValidXmlInfo())
		region = m_titleInfo_update.GetMetaInfo()->GetRegion();

	if (region == CafeConsoleRegion::JPN)
		defaultFileName.append(" (JP)");
	else if (region == CafeConsoleRegion::EUR)
		defaultFileName.append(" (EU)");
	else if (region == CafeConsoleRegion::USA)
		defaultFileName.append(" (US)");
	if (m_titleInfo_update.IsValid())
	{
		defaultFileName.append(fmt::format(" (v{})", m_titleInfo_update.GetAppTitleVersion()));
	}
	defaultFileName.append(".wua");

	return defaultFileName;
}

bool WuaConverter::ZArchiveWriterContext::RecursivelyAddFiles(std::string archivePath, std::string fscPath)
{
	sint32 fscStatus;
	std::unique_ptr<FSCVirtualFile> vfDir(fsc_openDirIterator(fscPath.c_str(), &fscStatus));
	if (!vfDir)
		return false;
	if (cancelled)
		return false;
	zaWriter->MakeDir(archivePath.c_str(), false);
	FSCDirEntry dirEntry;
	while (fsc_nextDir(vfDir.get(), &dirEntry))
	{
		if (dirEntry.isFile)
		{
			zaWriter->StartNewFile((archivePath + dirEntry.path).c_str());
			std::unique_ptr<FSCVirtualFile> vFile(fsc_open((fscPath + dirEntry.path).c_str(), FSC_ACCESS_FLAG::OPEN_FILE | FSC_ACCESS_FLAG::READ_PERMISSION, &fscStatus));
			if (!vFile)
				return false;
			transferBuffer.resize(32 * 1024); // 32KB
			uint32 readBytes;
			while (true)
			{
				readBytes = vFile->fscReadData(transferBuffer.data(), transferBuffer.size());
				if (readBytes == 0)
					break;
				zaWriter->AppendData(transferBuffer.data(), readBytes);
				if (cancelled)
					return false;
				transferredInputBytes += readBytes;
			}
		}
		else if (dirEntry.isDirectory)
		{
			if (!RecursivelyAddFiles(fmt::format("{}{}/", archivePath, dirEntry.path), fmt::format("{}{}/", fscPath, dirEntry.path)))
				return false;
		}
		else
		{
			cemu_assert_unimplemented();
		}
	}
	return true;
}

void WuaConverter::ZArchiveWriterContext::NewOutputFile(const int32_t partIndex, void* _ctx)
{
	auto ctx = (ZArchiveWriterContext*)_ctx;
	ctx->sink = std::make_unique<boost::iostreams::file_descriptor_sink>(ctx->fd, boost::iostreams::never_close_handle);
	ctx->isValid = ctx->sink->is_open();
}

void WuaConverter::ZArchiveWriterContext::WriteOutputData(const void* data, size_t length, void* _ctx)
{
	auto* ctx = (ZArchiveWriterContext*)_ctx;
	if (ctx->isValid)
		ctx->sink->write(reinterpret_cast<const char*>(data), length);
}

bool WuaConverter::ZArchiveWriterContext::StoreTitle(TitleInfo* titleInfo)
{
	std::string temporaryMountPath = TitleInfo::GetUniqueTempMountingPath();
	titleInfo->Mount(temporaryMountPath, "", FSC_PRIORITY_BASE);
	bool r = RecursivelyAddFiles(fmt::format("{:016x}_v{}/", titleInfo->GetAppTitleId(), titleInfo->GetAppTitleVersion()), temporaryMountPath);
	titleInfo->Unmount(temporaryMountPath);
	return r;
}

bool WuaConverter::ZArchiveWriterContext::AddTitles(TitleInfo** titles, size_t count)
{
	// store files
	for (size_t i = 0; i < count; i++)
	{
		if (!StoreTitle(titles[i]))
			return false;
	}
	return true;
}
