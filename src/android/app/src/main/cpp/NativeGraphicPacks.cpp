#include "Cafe/CafeSystem.h"
#include "config/CemuConfig.h"
#include "Cafe/GraphicPack/GraphicPack2.h"
#include "JNIUtils.h"

namespace NativeGraphicPacks
{
	std::unordered_map<sint64, GraphicPackPtr> s_graphicPacks;

	void SaveGraphicPackStateToConfig(GraphicPackPtr graphicPack)
	{
		auto& data = GetConfig();
		auto filename = _utf8ToPath(graphicPack->GetNormalizedPathString());
		if (data.graphic_pack_entries.contains(filename))
			data.graphic_pack_entries.erase(filename);
		if (graphicPack->IsEnabled())
		{
			data.graphic_pack_entries.try_emplace(filename);
			auto& it = data.graphic_pack_entries[filename];
			// otherwise store all selected presets
			for (const auto& preset : graphicPack->GetActivePresets())
				it.try_emplace(preset->category, preset->name);
		}
		else if (graphicPack->IsDefaultEnabled())
		{
			// save that its disabled
			data.graphic_pack_entries.try_emplace(filename);
			auto& it = data.graphic_pack_entries[filename];
			it.try_emplace("_disabled", "false");
		}
	}

	jobject GetGraphicPresets(JNIEnv* env, GraphicPackPtr graphicPack, sint64 id)
	{
		auto graphicPackPresetClass = env->FindClass("info/cemu/cemu/nativeinterface/NativeGraphicPacks$GraphicPackPreset");
		auto graphicPackPresetCtorId = env->GetMethodID(graphicPackPresetClass, "<init>", "(JLjava/lang/String;Ljava/util/ArrayList;Ljava/lang/String;)V");

		std::vector<std::string> order;
		auto presets = graphicPack->GetCategorizedPresets(order);

		std::vector<jobject> presetsJobjects;
		for (const auto& category : order)
		{
			const auto& entry = presets[category];
			// test if any preset is visible and update its status
			if (std::none_of(entry.cbegin(), entry.cend(), [graphicPack](const auto& p) { return p->visible; }))
			{
				continue;
			}

			jstring categoryJStr = category.empty() ? nullptr : JNIUtils::ToJString(env, category);
			std::vector<std::string> presetSelections;
			std::optional<std::string> activePreset;
			for (auto& pentry : entry)
			{
				if (!pentry->visible)
					continue;

				presetSelections.push_back(pentry->name);

				if (pentry->active)
					activePreset = pentry->name;
			}

			jstring activePresetJstr = nullptr;
			if (activePreset.has_value())
				activePresetJstr = JNIUtils::ToJString(env, activePreset.value());
			else if (!presetSelections.empty())
				activePresetJstr = JNIUtils::ToJString(env, presetSelections.front());
			auto presetJObject = env->NewObject(graphicPackPresetClass, graphicPackPresetCtorId, id, categoryJStr, JNIUtils::CreateJavaStringArrayList(env, presetSelections), activePresetJstr);
			presetsJobjects.push_back(presetJObject);
		}
		return JNIUtils::CreateArrayList(env, presetsJobjects);
	}
} // namespace NativeGraphicPacks

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeGraphicPacks_refreshGraphicPacks([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	if (CafeSystem::IsTitleRunning())
	{
		return;
	}

	GraphicPack2::ClearGraphicPacks();
	GraphicPack2::LoadAll();
	NativeGraphicPacks::s_graphicPacks.clear();

	auto graphicPacks = GraphicPack2::GetGraphicPacks();

	for (auto&& graphicPack : graphicPacks)
	{
		NativeGraphicPacks::s_graphicPacks[reinterpret_cast<sint64>(graphicPack.get())] = graphicPack;
	}
}

extern "C" [[maybe_unused]] JNIEXPORT jobject JNICALL
Java_info_cemu_cemu_nativeinterface_NativeGraphicPacks_getGraphicPackBasicInfos(JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	auto graphicPackInfoClass = env->FindClass("info/cemu/cemu/nativeinterface/NativeGraphicPacks$GraphicPackBasicInfo");
	auto graphicPackInfoCtorId = env->GetMethodID(graphicPackInfoClass, "<init>", "(JLjava/lang/String;ZLjava/util/ArrayList;)V");

	std::vector<jobject> graphicPackInfoJObjects;
	graphicPackInfoJObjects.reserve(NativeGraphicPacks::s_graphicPacks.size());

	for (auto&& graphicPack : NativeGraphicPacks::s_graphicPacks)
	{
		jstring virtualPath = JNIUtils::ToJString(env, graphicPack.second->GetVirtualPath());
		jlong id = graphicPack.first;
		jobject titleIds = JNIUtils::CreateJavaLongArrayList(env, graphicPack.second->GetTitleIds());
		jobject jGraphicPack = env->NewObject(graphicPackInfoClass, graphicPackInfoCtorId, id, virtualPath, graphicPack.second->IsEnabled(), titleIds);
		graphicPackInfoJObjects.push_back(jGraphicPack);
	}
	return JNIUtils::CreateArrayList(env, graphicPackInfoJObjects);
}

extern "C" [[maybe_unused]] JNIEXPORT jobject JNICALL
Java_info_cemu_cemu_nativeinterface_NativeGraphicPacks_getGraphicPack(JNIEnv* env, [[maybe_unused]] jclass clazz, jlong id)
{
	auto graphicPackClass = env->FindClass("info/cemu/cemu/nativeinterface/NativeGraphicPacks$GraphicPack");
	auto graphicPackCtorId = env->GetMethodID(graphicPackClass, "<init>", "(JZLjava/lang/String;Ljava/lang/String;Ljava/util/ArrayList;)V");
	auto graphicPack = NativeGraphicPacks::s_graphicPacks.at(id);

	jstring graphicPackName = JNIUtils::ToJString(env, graphicPack->GetName());
	jstring graphicPackDescription = JNIUtils::ToJString(env, graphicPack->GetDescription());
	return env->NewObject(
		graphicPackClass,
		graphicPackCtorId,
		id,
		graphicPack->IsEnabled(),
		graphicPackName,
		graphicPackDescription,
		NativeGraphicPacks::GetGraphicPresets(env, graphicPack, id));
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeGraphicPacks_setGraphicPackActive([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jlong id, jboolean active)
{
	auto graphicPack = NativeGraphicPacks::s_graphicPacks.at(id);
	graphicPack->SetEnabled(active);
	NativeGraphicPacks::SaveGraphicPackStateToConfig(graphicPack);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_cemu_nativeinterface_NativeGraphicPacks_setGraphicPackActivePreset([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jlong id, jstring category, jstring preset)
{
	std::string presetCategory = category == nullptr ? "" : JNIUtils::FromJString(env, category);
	auto graphicPack = NativeGraphicPacks::s_graphicPacks.at(id);
	graphicPack->SetActivePreset(presetCategory, JNIUtils::FromJString(env, preset));
	NativeGraphicPacks::SaveGraphicPackStateToConfig(graphicPack);
}

extern "C" [[maybe_unused]] JNIEXPORT jobject JNICALL
Java_info_cemu_cemu_nativeinterface_NativeGraphicPacks_getGraphicPackPresets(JNIEnv* env, [[maybe_unused]] jclass clazz, jlong id)
{
	return NativeGraphicPacks::GetGraphicPresets(env, NativeGraphicPacks::s_graphicPacks.at(id), id);
}
