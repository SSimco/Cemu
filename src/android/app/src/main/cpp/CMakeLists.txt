add_library(CemuAndroid SHARED
        AndroidAudio.cpp
        AndroidEmulatedController.cpp
        AndroidSwkbdCallbacks.cpp
        CompressTitleCallbacks.cpp
        GameTitleLoader.cpp
        Image.cpp
        JNIUtils.cpp
        NativeActiveSettings.cpp
        NativeAccount.cpp
        NativeEmulation.cpp
        NativeGameTitles.cpp
        NativeGraphicPacks.cpp
        NativeInput.cpp
        NativeLib.cpp
        NativeLocalization.cpp
        NativeLogging.cpp
        NativeSettings.cpp
        NativeSwkbd.cpp
        WuaConverter.cpp
)

target_link_libraries(CemuAndroid PRIVATE
        -landroid
        CemuCommon
        CemuAudio
        CemuComponents
        CemuCafe
        CemuBin
        CemuGui
        ZArchive::zarchive
        stb
)
