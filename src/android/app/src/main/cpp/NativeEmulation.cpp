#include "WindowSystem.h"
#include "JNIUtils.h"
#include "AndroidAudio.h"
#include "AndroidEmulatedController.h"
#include "AndroidFilesystemCallbacks.h"
#include "Cafe/HW/Latte/Core/LatteOverlay.h"
#include "Cafe/HW/Latte/Renderer/Vulkan/VulkanAPI.h"
#include "Cafe/HW/Latte/Renderer/Vulkan/VulkanRenderer.h"
#include "Cafe/CafeSystem.h"
#include "GameTitleLoader.h"
#include "input/ControllerFactory.h"
#include "input/InputManager.h"
#include "input/api/Android/AndroidController.h"
#include "input/api/Android/AndroidControllerProvider.h"
#include "config/ActiveSettings.h"
#include "Cemu/ncrypto/ncrypto.h"

// forward declaration from main.cpp
void CemuCommonInit();

namespace NativeEmulation
{
	void initializeAudioDevices()
	{
		auto& config = GetConfig();
		if (!config.tv_device.empty())
			AndroidAudio::createAudioDevice(IAudioAPI::AudioAPI::Cubeb, config.tv_channels, config.tv_volume, true);

		if (!config.pad_device.empty())
			AndroidAudio::createAudioDevice(IAudioAPI::AudioAPI::Cubeb, config.pad_channels, config.pad_volume, false);
	}

	void createCemuDirectories()
	{
		std::wstring mlc = ActiveSettings::GetMlcPath().generic_wstring();

		// create sys/usr folder in mlc01
		const auto sysFolder = fs::path(mlc).append(L"sys");
		fs::create_directories(sysFolder);

		const auto usrFolder = fs::path(mlc).append(L"usr");
		fs::create_directories(usrFolder);
		fs::create_directories(fs::path(usrFolder).append("title/00050000")); // base
		fs::create_directories(fs::path(usrFolder).append("title/0005000c")); // dlc
		fs::create_directories(fs::path(usrFolder).append("title/0005000e")); // update

		// Mii Maker save folders {0x500101004A000, 0x500101004A100, 0x500101004A200},
		fs::create_directories(fs::path(mlc).append(L"usr/save/00050010/1004a000/user/common/db"));
		fs::create_directories(fs::path(mlc).append(L"usr/save/00050010/1004a100/user/common/db"));
		fs::create_directories(fs::path(mlc).append(L"usr/save/00050010/1004a200/user/common/db"));

		// lang files
		auto langDir = fs::path(mlc).append(L"sys/title/0005001b/1005c000/content");
		fs::create_directories(langDir);

		auto langFile = fs::path(langDir).append("language.txt");
		if (!fs::exists(langFile))
		{
			std::ofstream file(langFile);
			if (file.is_open())
			{
				const char* langStrings[] = {"ja", "en", "fr", "de", "it", "es", "zh", "ko", "nl", "pt", "ru", "zh"};
				for (const char* lang : langStrings)
					file << fmt::format(R"("{}",)", lang) << std::endl;

				file.flush();
				file.close();
			}
		}

		auto countryFile = fs::path(langDir).append("country.txt");
		if (!fs::exists(countryFile))
		{
			std::ofstream file(countryFile);
			for (sint32 i = 0; i < 201; i++)
			{
				const char* countryCode = NCrypto::GetCountryAsString(i);
				if (boost::iequals(countryCode, "NN"))
					file << "NULL," << std::endl;
				else
					file << fmt::format(R"("{}",)", countryCode) << std::endl;
			}
			file.flush();
			file.close();
		}

		// cemu directories
		const auto controllerProfileFolder = ActiveSettings::GetConfigPath(L"controllerProfiles").generic_wstring();
		if (!fs::exists(controllerProfileFolder))
			fs::create_directories(controllerProfileFolder);

		const auto memorySearcherFolder = ActiveSettings::GetUserDataPath(L"memorySearcher").generic_wstring();
		if (!fs::exists(memorySearcherFolder))
			fs::create_directories(memorySearcherFolder);
	}

	enum PrepareTitleResult : sint32
	{
		SUCCESSFUL = 0,
		ERROR_GAME_BASE_FILES_NOT_FOUND = 1,
		ERROR_NO_DISC_KEY = 2,
		ERROR_NO_TITLE_TIK = 3,
		ERROR_UNKNOWN = 4,
	};

	std::shared_ptr<ANativeWindow> createANativeWindowFromSurface(JNIEnv* env, jobject surface)
	{
		ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
		return {window, &ANativeWindow_release};
	}

	class TestSurface
	{
	  public:
		TestSurface()
		{
			JNIUtils::ScopedJNIENV env;

			jclass surfaceTextureClass = env->FindClass("android/graphics/SurfaceTexture");
			jmethodID ctorSurfaceTexture = env->GetMethodID(surfaceTextureClass, "<init>", "(I)V");
			jobject localSurfaceTexture = env->NewObject(surfaceTextureClass, ctorSurfaceTexture, 0);

			jclass surfaceClass = env->FindClass("android/view/Surface");
			jmethodID ctorSurface = env->GetMethodID(surfaceClass, "<init>", "(Landroid/graphics/SurfaceTexture;)V");
			jobject localSurface = env->NewObject(surfaceClass, ctorSurface, localSurfaceTexture);

			m_surfaceTexture = env->NewGlobalRef(localSurfaceTexture);
			m_surface = env->NewGlobalRef(localSurface);

			m_window = ANativeWindow_fromSurface(*env, m_surface);
			ANativeWindow_acquire(m_window);

			env->DeleteLocalRef(localSurfaceTexture);
			env->DeleteLocalRef(localSurface);
			env->DeleteLocalRef(surfaceTextureClass);
			env->DeleteLocalRef(surfaceClass);
		}

		~TestSurface()
		{
			JNIUtils::ScopedJNIENV env;

			ANativeWindow_release(m_window);

			jclass surfaceClass = env->FindClass("android/view/Surface");
			jmethodID releaseSurface = env->GetMethodID(surfaceClass, "release", "()V");
			env->CallVoidMethod(m_surface, releaseSurface);
			env->DeleteGlobalRef(m_surface);
			m_surface = nullptr;
			env->DeleteLocalRef(surfaceClass);

			jclass surfaceTextureClass = env->FindClass("android/graphics/SurfaceTexture");
			jmethodID releaseSurfaceTexture = env->GetMethodID(surfaceTextureClass, "release", "()V");
			env->CallVoidMethod(m_surfaceTexture, releaseSurfaceTexture);
			env->DeleteGlobalRef(m_surfaceTexture);
			m_surfaceTexture = nullptr;
			env->DeleteLocalRef(surfaceTextureClass);
		}

		ANativeWindow* getWindow()
		{
			return m_window;
		}

	  private:
		ANativeWindow* m_window;
		jobject m_surface = nullptr;
		jobject m_surfaceTexture = nullptr;
	};

	std::unique_ptr<TestSurface> g_testSurface;
} // namespace NativeEmulation

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_setReplaceTVWithPadView([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean swapped)
{
	// Emulate pressing the TAB key for showing DRC instead of TV
	WindowSystem::GetWindowInfo().set_keystate(static_cast<uint32>(WindowSystem::PlatformKeyCodes::TAB), swapped);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_initializeEmulation([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	FilesystemAndroid::SetFilesystemCallbacks(std::make_shared<AndroidFilesystemCallbacks>());
	GetConfigHandle().SetFilename(ActiveSettings::GetConfigPath("settings.xml").generic_wstring());
	NativeEmulation::createCemuDirectories();
	NetworkConfig::LoadOnce();
	ActiveSettings::Init();
	LatteOverlay_init();
	CemuCommonInit();
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_initializeRenderer(JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	InitializeGlobalVulkan();
	JNIUtils::handleNativeException(env, [&]() {
		NativeEmulation::g_testSurface = std::make_unique<NativeEmulation::TestSurface>();

		WindowSystem::GetWindowInfo().window_main.surface = NativeEmulation::g_testSurface->getWindow();

		g_renderer = std::make_unique<VulkanRenderer>();
	});
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_setDPI([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jfloat dpi)
{
	auto& windowInfo = WindowSystem::GetWindowInfo();
	windowInfo.dpi_scale = windowInfo.pad_dpi_scale = dpi;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_clearPadSurface([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	VulkanRenderer::GetInstance()->StopUsingPadAndWait();
	WindowSystem::GetWindowInfo().pad_open = false;
}


extern "C" [[maybe_unused]] JNIEXPORT jboolean JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_supportsLoadingCustomDriver([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	return SupportsLoadingCustomDriver();
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_setSurface(JNIEnv* env, [[maybe_unused]] jclass clazz, jobject surface, jboolean is_main_canvas)
{
	JNIUtils::handleNativeException(env, [&]() {
		auto& windowHandleInfo = is_main_canvas ? WindowSystem::GetWindowInfo().canvas_main : WindowSystem::GetWindowInfo().canvas_pad;
		auto oldWindow = windowHandleInfo.surface.load();
		if (oldWindow != nullptr)
			ANativeWindow_release(static_cast<ANativeWindow*>(oldWindow));
		auto newSurface = ANativeWindow_fromSurface(env, surface);
		ANativeWindow_acquire(newSurface);
		windowHandleInfo.surface = newSurface;
		windowHandleInfo.surface.notify_all();
	});
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_initializeSurface(JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean is_main_canvas)
{
	JNIUtils::handleNativeException(env, [&]() {
		int width, height;
		if (is_main_canvas)
		{
			WindowSystem::GetWindowPhysSize(width, height);
		}
		else
		{
			WindowSystem::GetPadWindowPhysSize(width, height);
			WindowSystem::GetWindowInfo().pad_open = true;
		}

		VulkanRenderer::GetInstance()->InitializeSurface({width, height}, is_main_canvas);
	});
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_setSurfaceSize([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint width, jint height, jboolean is_main_canvas)
{
	auto& windowInfo = WindowSystem::GetWindowInfo();
	if (is_main_canvas)
	{
		windowInfo.width = windowInfo.phys_width = width;
		windowInfo.height = windowInfo.phys_height = height;
	}
	else
	{
		windowInfo.pad_width = windowInfo.phys_pad_width = width;
		windowInfo.pad_height = windowInfo.phys_pad_height = height;
	}
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_initializeSystems([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	WindowSystem::GetWindowInfo().set_keystatesup();
	NativeEmulation::initializeAudioDevices();
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_prepareTitle([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jstring launch_path)
{
	fs::path launchPath = JNIUtils::toString(env, launch_path);

	TitleInfo launchTitle{launchPath};

	using enum NativeEmulation::PrepareTitleResult;

	if (launchTitle.IsValid())
	{
		// the title might not be in the TitleList, so we add it as a temporary entry
		CafeTitleList::AddTitleFromPath(launchPath);
		// title is valid, launch from TitleId
		TitleId baseTitleId;
		if (!CafeTitleList::FindBaseTitleId(launchTitle.GetAppTitleId(), baseTitleId))
		{
			return ERROR_GAME_BASE_FILES_NOT_FOUND;
		}
		CafeSystem::PREPARE_STATUS_CODE r = CafeSystem::PrepareForegroundTitle(baseTitleId);
		if (r != CafeSystem::PREPARE_STATUS_CODE::SUCCESS)
		{
			return ERROR_UNKNOWN;
		}
	}
	else // if (launchTitle.GetFormat() == TitleInfo::TitleDataFormat::INVALID_STRUCTURE )
	{
		// title is invalid, if it's an RPX/ELF we can launch it directly
		// otherwise it's an error
		CafeTitleFileType fileType = DetermineCafeSystemFileType(launchPath);
		if (fileType == CafeTitleFileType::RPX || fileType == CafeTitleFileType::ELF)
		{
			CafeSystem::PREPARE_STATUS_CODE r = CafeSystem::PrepareForegroundTitleFromStandaloneRPX(launchPath);
			if (r != CafeSystem::PREPARE_STATUS_CODE::SUCCESS)
			{
				return ERROR_UNKNOWN;
			}
		}
		else if (launchTitle.GetInvalidReason() == TitleInfo::InvalidReason::NO_DISC_KEY)
		{
			return ERROR_NO_DISC_KEY;
		}
		else if (launchTitle.GetInvalidReason() == TitleInfo::InvalidReason::NO_TITLE_TIK)
		{
			return ERROR_NO_TITLE_TIK;
		}
		else
		{
			return ERROR_UNKNOWN;
		}
	}

	return SUCCESSFUL;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_launchTitle([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	CafeSystem::LaunchForegroundTitle();
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_pauseTitle([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	CafeSystem::PauseTitle();
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeEmulation_resumeTitle([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	CafeSystem::ResumeTitle();
}
