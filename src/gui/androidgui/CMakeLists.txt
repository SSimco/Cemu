add_library(CemuAndroidGui
    AndroidWindowSystem.cpp
    AndroidWindowSystem.h
)

target_link_libraries(CemuAndroidGui PRIVATE
	CemuAudio
	CemuCafe
	CemuCommon
	CemuComponents
	CemuConfig
	CemuInput
	CemuResource
	CemuUtil
)