cmake_minimum_required(VERSION 3.25)

project(libucontext LANGUAGES C ASM)

option(FREESTANDING "Do not use system headers" ON)
option(EXPORT_UNPREFIXED "Export POSIX 2004 ucontext names as alises" OFF)

if(CMAKE_SYSTEM_PROCESSOR MATCHES "(x86)|(X86)|(amd64)|(AMD64)")
    set(ARCH "x86_64")
elseif(CMAKE_SYSTEM_PROCESSOR MATCHES "(aarch64)|(AARCH64)")
    set(ARCH "aarch64")
endif()

add_library(libucontext)

target_sources(
    libucontext
    PRIVATE
        libucontext/arch/${ARCH}/getcontext.S
        libucontext/arch/${ARCH}/setcontext.S
        libucontext/arch/${ARCH}/swapcontext.S
)

if(${ARCH} STREQUAL "mips" OR ${ARCH} STREQUAL "mips64")
    target_sources(libucontext PRIVATE libucontext/arch/${ARCH}/makecontext.S)
else()
    target_sources(libucontext PRIVATE libucontext/arch/${ARCH}/makecontext.c)
endif()

set(ARCH_WITH_STARTCONTEXT
    "mips"
    "mips64"
    "ppc"
    "ppc64"
    "s390x"
)

if(${ARCH} IN_LIST ARCH_WITH_STARTCONTEXT)
    target_sources(libucontext PRIVATE libucontext/arch/${ARCH}/startcontext.S)
else()
    target_sources(libucontext PRIVATE libucontext/arch/${ARCH}/trampoline.c)
endif()

set(ARCH_WITH_RETFROMSYSCALL "ppc" "ppc64")

if(${ARCH} IN_LIST ARCH_WITH_RETFROMSYSCALL)
    target_sources(
        libucontext
        PRIVATE libucontext/arch/${ARCH}/retfromsyscall.c
    )
endif()

if(FREESTANDING)
    set(EXPORT_UNPREFIXED OFF)
    target_compile_definitions(libucontext PRIVATE FREESTANDING)
    target_include_directories(
        libucontext
        PUBLIC libucontext/arch/${ARCH}/include
    )
else()
    target_include_directories(
        libucontext
        PUBLIC libucontext/arch/common/include
    )
endif()

if(EXPORT_UNPREFIXED)
    target_compile_definitions(libucontext PRIVATE EXPORT_UNPREFIXED)
endif()

target_compile_definitions(libucontext PRIVATE _DEFAULT_SOURCE)

target_include_directories(libucontext PRIVATE libucontext/arch/common)

target_include_directories(libucontext PUBLIC libucontext/include)
