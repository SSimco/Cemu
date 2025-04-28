#pragma once

#if defined(STUB_RECOMPILER_ASM_UTIL)

static void recompiler_fres()
{
	cemu_assert_unimplemented();
}
static void recompiler_frsqrte()
{
	cemu_assert_unimplemented();
}

#else

extern "C" void recompiler_fres();
extern "C" void recompiler_frsqrte();

#endif
