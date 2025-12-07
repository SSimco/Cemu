#include "cpu_features.h"

// wrappers with uniform prototype for implementation-specific x86 CPU id
#if defined(ARCH_X86_64)
#ifdef __GNUC__
#include <cpuid.h>
#endif

inline void cpuid(int cpuInfo[4], int functionId) {
#if defined(_MSC_VER)
	__cpuid(cpuInfo, functionId);
#elif defined(__GNUC__)
	__cpuid(functionId, cpuInfo[0], cpuInfo[1], cpuInfo[2], cpuInfo[3]);
#else
#error No definition for cpuid
#endif
}

inline void cpuidex(int cpuInfo[4], int functionId, int subFunctionId) {
#if defined(_MSC_VER)
	__cpuidex(cpuInfo, functionId, subFunctionId);
#elif defined(__GNUC__)
	__cpuid_count(functionId, subFunctionId, cpuInfo[0], cpuInfo[1], cpuInfo[2], cpuInfo[3]);
#else
#error No definition for cpuidex
#endif
}
#endif

#if defined(__aarch64__)
#if BOOST_OS_LINUX
std::string getCpuBrandNameLinux()
{
	static auto default_name = "unknown";
	std::ifstream ifstream("/proc/device-tree/model");
	if (!ifstream.is_open())
		return default_name;
	std::stringstream stringstream;
	stringstream << ifstream.rdbuf();
	std::string model = stringstream.str();
	if (model.empty())
		return default_name;
	return model;
}
#if BOOST_PLAT_ANDROID

#include <sys/system_properties.h>

std::string getProperty(const std::string& name)
{
	const prop_info* pi = __system_property_find(name.c_str());
	std::string propValue;
	if (pi == nullptr)
		return {};
	__system_property_read_callback(
		pi,
		[](void* cookie, const char* name, const char* value, uint32_t serial) {
			if (cookie == nullptr)
				return;
			*reinterpret_cast<std::string*>(cookie) = value;
		},
		&propValue);
	return propValue;
}

std::string getCpuBrandNameAndroid()
{
	static auto propertiesNames = {
		"ro.soc.manufacturer",
		"ro.soc.model",
		"ro.boot.hardware.revision",
	};
    std::string tmp;
	for (auto&& propertyName : propertiesNames)
	{
		auto propertyValue = getProperty(propertyName);
		if (!propertyValue.empty())
        {
            if (!tmp.empty())
                tmp.append(", ");
            tmp.append(propertyValue);
        }
	}
	if (tmp.empty())
		return getCpuBrandNameLinux();
    return tmp;
}
#endif // BOOST_PLAT_ANDROID
#endif // BOOST_OS_LINUX
#endif // defined(__aarch64__)

CPUFeaturesImpl::CPUFeaturesImpl()
{
#if defined(__aarch64__)
#if BOOST_OS_LINUX
#if BOOST_PLAT_ANDROID
	m_cpuBrandName = getCpuBrandNameAndroid();
#else
	m_cpuBrandName = getCpuBrandNameLinux();
#endif // BOOST_PLAT_ANDROID
#endif // BOOST_OS_LINUX
#endif // defined(__aarch64__)

#if defined(ARCH_X86_64)
	int cpuInfo[4];
	cpuid(cpuInfo, 0x80000001);
	x86.lzcnt = ((cpuInfo[2] >> 5) & 1) != 0;
	cpuid(cpuInfo, 0x1);
	x86.movbe = ((cpuInfo[2] >> 22) & 1) != 0;
	x86.avx = ((cpuInfo[2] >> 28) & 1) != 0;
	x86.aesni = ((cpuInfo[2] >> 25) & 1) != 0;
	x86.ssse3 = ((cpuInfo[2] >> 9) & 1) != 0;
	x86.sse4_1 = ((cpuInfo[2] >> 19) & 1) != 0;
	cpuidex(cpuInfo, 0x7, 0);
	x86.avx2 = ((cpuInfo[1] >> 5) & 1) != 0;
	x86.bmi2 = ((cpuInfo[1] >> 8) & 1) != 0;
	cpuid(cpuInfo, 0x80000007);
	x86.invariant_tsc = ((cpuInfo[3] >> 8) & 1);
	// get CPU brand name
	uint32_t nExIds, i = 0;
	char cpuBrandName[0x40]{ 0 };
	memset(cpuBrandName, 0, sizeof(cpuBrandName));
	cpuid(cpuInfo, 0x80000000);
	nExIds = (uint32_t)cpuInfo[0];
	for (uint32_t i = 0x80000000; i <= nExIds; ++i)
	{
		cpuid(cpuInfo, i);
		if (i == 0x80000002)
			memcpy(cpuBrandName, cpuInfo, sizeof(cpuInfo));
		else if (i == 0x80000003)
			memcpy(cpuBrandName + 16, cpuInfo, sizeof(cpuInfo));
		else if (i == 0x80000004)
			memcpy(cpuBrandName + 32, cpuInfo, sizeof(cpuInfo));
	}
	m_cpuBrandName = cpuBrandName;
#endif
}

std::string CPUFeaturesImpl::GetCPUName()
{
	return { m_cpuBrandName };
}

std::string CPUFeaturesImpl::GetCommaSeparatedExtensionList()
{
	std::string tmp;
	auto appendExt = [&tmp](const char* str)
	{
		if (!tmp.empty())
			tmp.append(", ");
		tmp.append(str);
	};
	if (x86.ssse3)
		appendExt("SSSE3");
	if (x86.sse4_1)
		appendExt("SSE4.1");
	if (x86.avx)
		appendExt("AVX");
	if (x86.avx2)
		appendExt("AVX2");
	if (x86.lzcnt)
		appendExt("LZCNT");
	if (x86.movbe)
		appendExt("MOVBE");
	if (x86.bmi2)
		appendExt("BMI2");
	if (x86.aesni)
		appendExt("AES-NI");
	if(x86.invariant_tsc)
		appendExt("INVARIANT-TSC");
	return tmp;
}

CPUFeaturesImpl g_CPUFeatures;