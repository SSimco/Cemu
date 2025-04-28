#include "BackendAArch64.h"

#pragma push_macro("CSIZE")
#undef CSIZE
#include <xbyak_aarch64.h>
#pragma pop_macro("CSIZE")
#include <xbyak_aarch64_util.h>

#include <cstddef>

#include "asm/x64util.h"
#include "../PPCRecompiler.h"
#include "Common/precompiled.h"
#include "Common/cpu_features.h"
#include "HW/Espresso/Interpreter/PPCInterpreterInternal.h"
#include "HW/Espresso/Interpreter/PPCInterpreterHelper.h"
#include "HW/Espresso/PPCState.h"

using namespace Xbyak_aarch64;

constexpr uint32_t TEMP_GPR_1_ID = 25;
constexpr uint32_t TEMP_GPR_2_ID = 26;
constexpr uint32_t PPC_RECOMPILER_INSTANCE_DATA_REG_ID = 27;
constexpr uint32_t MEMORY_BASE_REG_ID = 28;
constexpr uint32_t HCPU_REG_ID = 29;

constexpr uint32_t TEMP_FPR_1_ID = 29;
constexpr uint32_t TEMP_FPR_2_ID = 30;
constexpr uint32_t TEMP_FPR_3_ID = 31;
constexpr uint32_t ASM_ROUTINE_FPR_ID = 31;

struct FPReg
{
	explicit FPReg(size_t index)
		: index(index), VReg(index), DReg(index), SReg(index), HReg(index), QReg(index), BReg(index)
	{
	}
	const size_t index;
	const VReg VReg;
	const QReg QReg;
	const DReg DReg;
	const SReg SReg;
	const HReg HReg;
	const BReg BReg;
};

struct GPReg
{
	explicit GPReg(size_t index)
		: index(index), XReg(index), WReg(index)
	{
	}
	const size_t index;
	const XReg XReg;
	const WReg WReg;
};
constexpr uint64_t DOUBLE_1_0 = std::bit_cast<uint64_t>(1.0);
static const XReg HCPU_REG{HCPU_REG_ID}, PPC_REC_INSTANCE_REG{PPC_RECOMPILER_INSTANCE_DATA_REG_ID}, MEM_BASE_REG{MEMORY_BASE_REG_ID};
static const GPReg TEMP_GPR1{TEMP_GPR_1_ID};
static const GPReg TEMP_GPR2{TEMP_GPR_2_ID};
static const GPReg LR{TEMP_GPR_2_ID};

static const FPReg TEMP_FPR1{TEMP_FPR_1_ID};
static const FPReg TEMP_FPR2{TEMP_FPR_2_ID};
static const FPReg TEMP_FPR3{TEMP_FPR_3_ID};
static const FPReg ASM_ROUTINE_FPR{ASM_ROUTINE_FPR_ID};

static const util::Cpu s_cpu;

class AArch64Allocator : public Allocator
{
  private:
#ifdef XBYAK_USE_MMAP_ALLOCATOR
	inline static MmapAllocator s_allocator;
#else
	inline static Allocator s_allocator;
#endif
	Allocator* m_allocatorImpl;
	bool m_freeDisabled = false;

  public:
	AArch64Allocator()
		: m_allocatorImpl(reinterpret_cast<Allocator*>(&s_allocator)) {}

	uint32_t* alloc(size_t size) override
	{
		return m_allocatorImpl->alloc(size);
	}

	void setFreeDisabled(bool disabled)
	{
		m_freeDisabled = disabled;
	}

	void free(uint32_t* p) override
	{
		if (!m_freeDisabled)
			m_allocatorImpl->free(p);
	}

	[[nodiscard]] bool useProtect() const override
	{
		return !m_freeDisabled && m_allocatorImpl->useProtect();
	}
};

struct UnconditionalJumpInfo
{
	IMLSegment* target;
};

struct ConditionalRegJumpInfo
{
	IMLSegment* target;
	WReg regBool;
	bool mustBeTrue;
};

struct NegativeRegValueJumpInfo
{
	IMLSegment* target;
	WReg regValue;
};

using JumpInfo = std::variant<
	UnconditionalJumpInfo,
	ConditionalRegJumpInfo,
	NegativeRegValueJumpInfo>;

struct AArch64GenContext_t : CodeGenerator
{
	explicit AArch64GenContext_t(Allocator* allocator = nullptr);
	void enterRecompilerCode();
	void leaveRecompilerCode();

	void r_name(IMLInstruction* imlInstruction);
	void name_r(IMLInstruction* imlInstruction);
	bool r_s32(IMLInstruction* imlInstruction);
	bool r_r(IMLInstruction* imlInstruction);
	bool r_r_s32(IMLInstruction* imlInstruction);
	bool r_r_s32_carry(IMLInstruction* imlInstruction);
	bool r_r_r(IMLInstruction* imlInstruction);
	bool r_r_r_carry(IMLInstruction* imlInstruction);
	void compare(IMLInstruction* imlInstruction);
	void compare_s32(IMLInstruction* imlInstruction);
	bool load(IMLInstruction* imlInstruction, bool indexed);
	bool store(IMLInstruction* imlInstruction, bool indexed);
	void atomic_cmp_store(IMLInstruction* imlInstruction);
	bool macro(IMLInstruction* imlInstruction);
	void call_imm(IMLInstruction* imlInstruction);
	bool fpr_load(IMLInstruction* imlInstruction, bool indexed);
	void psq_load(uint8 mode, VReg& dataVReg, WReg& memReg, WReg& indexReg, sint32 memImmS32, bool indexed, const IMLReg& registerGQR = IMLREG_INVALID);
	void psq_load_generic(uint8 mode, VReg& dataReg, WReg& memReg, WReg& indexReg, sint32 memImmS32, bool indexed, const IMLReg& registerGQR);
	bool fpr_store(IMLInstruction* imlInstruction, bool indexed);
	void psq_store(uint8 mode, const IMLReg& dataReg, WReg& memReg, WReg& indexReg, sint32 memOffset, bool indexed, const IMLReg& registerGQR = IMLREG_INVALID);
	void psq_store_generic(uint8 mode, const IMLReg& dataReg, WReg& memReg, WReg& indexReg, sint32 memOffset, bool indexed, const IMLReg& registerGQR);
	void fpr_r_r(IMLInstruction* imlInstruction);
	void fpr_r_r_r(IMLInstruction* imlInstruction);
	void fpr_r_r_r_r(IMLInstruction* imlInstruction);
	void fpr_r(IMLInstruction* imlInstruction);
	void fpr_compare(IMLInstruction* imlInstruction);
	void cjump(IMLInstruction* imlInstruction, IMLSegment* imlSegment);
	void jump(IMLSegment* imlSegment);
	void conditionalJumpCycleCheck(IMLSegment* imlSegment);

	void gqr_generateScaleCode(const VReg& resReg, const VReg& dataReg, bool isLoad, bool scalePS1, const IMLReg& registerGQR);
	static constexpr size_t MAX_JUMP_INSTR_COUNT = 2;
	std::list<std::pair<size_t, JumpInfo>> jumps;
	void prepareJump(JumpInfo&& jumpInfo)
	{
		jumps.emplace_back(getSize(), jumpInfo);
		for (int i = 0; i < MAX_JUMP_INSTR_COUNT; ++i)
			nop();
	}

	std::map<IMLSegment*, size_t> segmentStarts;
	void storeSegmentStart(IMLSegment* imlSegment)
	{
		segmentStarts[imlSegment] = getSize();
	}

	bool processAllJumps()
	{
		for (auto&& [jumpStart, jumpInfo] : jumps)
		{
			bool success = std::visit(
				[&, this](const auto& jump) {
					setSize(jumpStart);
					sint64 targetAddress = segmentStarts.at(jump.target);
					sint64 addressOffset = targetAddress - jumpStart;
					return handleJump(addressOffset, jump);
				},
				jumpInfo);
			if (!success)
			{
				return false;
			}
		}
		return true;
	}

	bool handleJump(sint64 addressOffset, const UnconditionalJumpInfo& jump)
	{
		// in +/-128MB
		if (-0x8000000 <= addressOffset && addressOffset <= 0x7ffffff)
		{
			b(addressOffset);
			return true;
		}

		cemu_assert_suspicious();

		return false;
	}

	bool handleJump(sint64 addressOffset, const ConditionalRegJumpInfo& jump)
	{
		bool mustBeTrue = jump.mustBeTrue;

		// in +/-32KB
		if (-0x8000 <= addressOffset && addressOffset <= 0x7fff)
		{
			if (mustBeTrue)
				tbnz(jump.regBool, 0, addressOffset);
			else
				tbz(jump.regBool, 0, addressOffset);
			return true;
		}

		// in +/-1MB
		if (-0x100000 <= addressOffset && addressOffset <= 0xfffff)
		{
			if (mustBeTrue)
				cbnz(jump.regBool, addressOffset);
			else
				cbz(jump.regBool, addressOffset);
			return true;
		}

		Label skipJump;
		if (mustBeTrue)
			tbz(jump.regBool, 0, skipJump);
		else
			tbnz(jump.regBool, 0, skipJump);
		addressOffset -= 4;

		// in +/-128MB
		if (-0x8000000 <= addressOffset && addressOffset <= 0x7ffffff)
		{
			b(addressOffset);
			L(skipJump);
			return true;
		}

		cemu_assert_suspicious();

		return false;
	}

	bool handleJump(sint64 addressOffset, const NegativeRegValueJumpInfo& jump)
	{
		// in +/-32KB
		if (-0x8000 <= addressOffset && addressOffset <= 0x7fff)
		{
			tbnz(jump.regValue, 31, addressOffset);
			return true;
		}

		// in +/-1MB
		if (-0x100000 <= addressOffset && addressOffset <= 0xfffff)
		{
			tst(jump.regValue, 0x80000000);
			addressOffset -= 4;
			bne(addressOffset);
			return true;
		}

		Label skipJump;
		tbz(jump.regValue, 31, skipJump);
		addressOffset -= 4;

		// in +/-128MB
		if (-0x8000000 <= addressOffset && addressOffset <= 0x7ffffff)
		{
			b(addressOffset);
			L(skipJump);
			return true;
		}

		cemu_assert_suspicious();

		return false;
	}
};

template<typename T>
T fpReg(const IMLReg& imlReg)
{
	return T(imlReg.GetRegID() - IMLArchAArch64::PHYSREG_FPR_BASE);
}

template<typename T>
T gpReg(const IMLReg& imlReg)
{
	return T(imlReg.GetRegID() - IMLArchAArch64::PHYSREG_GPR_BASE);
}

AArch64GenContext_t::AArch64GenContext_t(Allocator* allocator)
	: CodeGenerator(DEFAULT_MAX_CODE_SIZE, AutoGrow, allocator)
{
}

void AArch64GenContext_t::r_name(IMLInstruction* imlInstruction)
{
	uint32 name = imlInstruction->op_r_name.name;

	if (imlInstruction->op_r_name.regR.GetBaseFormat() == IMLRegFormat::I64)
	{
		WReg regR = gpReg<WReg>(imlInstruction->op_r_name.regR);
		if (name >= PPCREC_NAME_R0 && name < PPCREC_NAME_R0 + 32)
		{
			ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, gpr) + sizeof(uint32) * (name - PPCREC_NAME_R0)));
		}
		else if (name >= PPCREC_NAME_SPR0 && name < PPCREC_NAME_SPR0 + 999)
		{
			uint32 sprIndex = (name - PPCREC_NAME_SPR0);
			if (sprIndex == SPR_LR)
				ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.LR)));
			else if (sprIndex == SPR_CTR)
				ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.CTR)));
			else if (sprIndex == SPR_XER)
				ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.XER)));
			else if (sprIndex >= SPR_UGQR0 && sprIndex <= SPR_UGQR7)
				ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.UGQR) + sizeof(PPCInterpreter_t::spr.UGQR[0]) * (sprIndex - SPR_UGQR0)));
			else
				cemu_assert_suspicious();
		}
		else if (name >= PPCREC_NAME_TEMPORARY && name < PPCREC_NAME_TEMPORARY + 4)
		{
			ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, temporaryGPR_reg) + sizeof(uint32) * (name - PPCREC_NAME_TEMPORARY)));
		}
		else if (name == PPCREC_NAME_XER_CA)
		{
			ldrb(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, xer_ca)));
		}
		else if (name == PPCREC_NAME_XER_SO)
		{
			ldrb(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, xer_so)));
		}
		else if (name >= PPCREC_NAME_CR && name <= PPCREC_NAME_CR_LAST)
		{
			ldrb(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, cr) + (name - PPCREC_NAME_CR)));
		}
		else if (name == PPCREC_NAME_CPU_MEMRES_EA)
		{
			ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, reservedMemAddr)));
		}
		else if (name == PPCREC_NAME_CPU_MEMRES_VAL)
		{
			ldr(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, reservedMemValue)));
		}
		else
		{
			cemu_assert_suspicious();
		}
	}
	else if (imlInstruction->op_r_name.regR.GetBaseFormat() == IMLRegFormat::F64)
	{
		QReg regR = fpReg<QReg>(imlInstruction->op_r_name.regR);
		if (name >= PPCREC_NAME_FPR0 && name < (PPCREC_NAME_FPR0 + 32))
		{
			mov(TEMP_GPR1.XReg, offsetof(PPCInterpreter_t, fpr) + sizeof(FPR_t) * (name - PPCREC_NAME_FPR0));
			ldr(regR, AdrReg(HCPU_REG, TEMP_GPR1.XReg));
		}
		else if (name >= PPCREC_NAME_TEMPORARY_FPR0 && name < (PPCREC_NAME_TEMPORARY_FPR0 + 8))
		{
			mov(TEMP_GPR1.XReg, offsetof(PPCInterpreter_t, temporaryFPR) + sizeof(FPR_t) * (name - PPCREC_NAME_TEMPORARY_FPR0));
			ldr(regR, AdrReg(HCPU_REG, TEMP_GPR1.XReg));
		}
		else
		{
			cemu_assert_debug(false);
		}
	}
	else
	{
		cemu_assert_suspicious();
	}
}

void AArch64GenContext_t::name_r(IMLInstruction* imlInstruction)
{
	uint32 name = imlInstruction->op_r_name.name;

	if (imlInstruction->op_r_name.regR.GetBaseFormat() == IMLRegFormat::I64)
	{
		auto regR = gpReg<WReg>(imlInstruction->op_r_name.regR);
		if (name >= PPCREC_NAME_R0 && name < PPCREC_NAME_R0 + 32)
		{
			str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, gpr) + sizeof(uint32) * (name - PPCREC_NAME_R0)));
		}
		else if (name >= PPCREC_NAME_SPR0 && name < PPCREC_NAME_SPR0 + 999)
		{
			uint32 sprIndex = (name - PPCREC_NAME_SPR0);
			if (sprIndex == SPR_LR)
				str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.LR)));
			else if (sprIndex == SPR_CTR)
				str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.CTR)));
			else if (sprIndex == SPR_XER)
				str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.XER)));
			else if (sprIndex >= SPR_UGQR0 && sprIndex <= SPR_UGQR7)
				str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.UGQR) + sizeof(PPCInterpreter_t::spr.UGQR[0]) * (sprIndex - SPR_UGQR0)));
			else
				cemu_assert_suspicious();
		}
		else if (name >= PPCREC_NAME_TEMPORARY && name < PPCREC_NAME_TEMPORARY + 4)
		{
			str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, temporaryGPR_reg) + sizeof(uint32) * (name - PPCREC_NAME_TEMPORARY)));
		}
		else if (name == PPCREC_NAME_XER_CA)
		{
			strb(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, xer_ca)));
		}
		else if (name == PPCREC_NAME_XER_SO)
		{
			strb(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, xer_so)));
		}
		else if (name >= PPCREC_NAME_CR && name <= PPCREC_NAME_CR_LAST)
		{
			strb(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, cr) + (name - PPCREC_NAME_CR)));
		}
		else if (name == PPCREC_NAME_CPU_MEMRES_EA)
		{
			str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, reservedMemAddr)));
		}
		else if (name == PPCREC_NAME_CPU_MEMRES_VAL)
		{
			str(regR, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, reservedMemValue)));
		}
		else
		{
			cemu_assert_suspicious();
		}
	}
	else if (imlInstruction->op_r_name.regR.GetBaseFormat() == IMLRegFormat::F64)
	{
		QReg regR = fpReg<QReg>(imlInstruction->op_r_name.regR);
		if (name >= PPCREC_NAME_FPR0 && name < (PPCREC_NAME_FPR0 + 32))
		{
			mov(TEMP_GPR1.XReg, offsetof(PPCInterpreter_t, fpr) + sizeof(FPR_t) * (name - PPCREC_NAME_FPR0));
			str(regR, AdrReg(HCPU_REG, TEMP_GPR1.XReg));
		}
		else if (name >= PPCREC_NAME_TEMPORARY_FPR0 && name < (PPCREC_NAME_TEMPORARY_FPR0 + 8))
		{
			mov(TEMP_GPR1.XReg, offsetof(PPCInterpreter_t, temporaryFPR) + sizeof(FPR_t) * (name - PPCREC_NAME_TEMPORARY_FPR0));
			str(regR, AdrReg(HCPU_REG, TEMP_GPR1.XReg));
		}
		else
		{
			cemu_assert_debug(false);
		}
	}
	else
	{
		cemu_assert_suspicious();
	}
}

bool AArch64GenContext_t::r_r(IMLInstruction* imlInstruction)
{
	WReg regR = gpReg<WReg>(imlInstruction->op_r_r.regR);
	WReg regA = gpReg<WReg>(imlInstruction->op_r_r.regA);

	if (imlInstruction->operation == PPCREC_IML_OP_ASSIGN)
	{
		mov(regR, regA);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_ENDIAN_SWAP)
	{
		rev(regR, regA);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_ASSIGN_S8_TO_S32)
	{
		sxtb(regR, regA);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_ASSIGN_S16_TO_S32)
	{
		sxth(regR, regA);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_NOT)
	{
		mvn(regR, regA);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_NEG)
	{
		neg(regR, regA);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_CNTLZW)
	{
		clz(regR, regA);
	}
	else
	{
		cemuLog_log(LogType::Recompiler, "PPCRecompilerAArch64Gen_imlInstruction_r_r(): Unsupported operation {:x}", imlInstruction->operation);
		return false;
	}
	return true;
}

bool AArch64GenContext_t::r_s32(IMLInstruction* imlInstruction)
{
	sint32 imm32 = imlInstruction->op_r_immS32.immS32;
	WReg reg = gpReg<WReg>(imlInstruction->op_r_immS32.regR);

	if (imlInstruction->operation == PPCREC_IML_OP_ASSIGN)
	{
		mov(reg, imm32);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_LEFT_ROTATE)
	{
		ror(reg, reg, 32 - (imm32 & 0x1f));
	}
	else
	{
		cemuLog_log(LogType::Recompiler, "PPCRecompilerAArch64Gen_imlInstruction_r_s32(): Unsupported operation {:x}", imlInstruction->operation);
		return false;
	}
	return true;
}

bool AArch64GenContext_t::r_r_s32(IMLInstruction* imlInstruction)
{
	WReg regR = gpReg<WReg>(imlInstruction->op_r_r_s32.regR);
	WReg regA = gpReg<WReg>(imlInstruction->op_r_r_s32.regA);
	sint32 immS32 = imlInstruction->op_r_r_s32.immS32;

	if (imlInstruction->operation == PPCREC_IML_OP_ADD)
	{
		add_imm(regR, regA, immS32, TEMP_GPR1.WReg);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_SUB)
	{
		sub_imm(regR, regA, immS32, TEMP_GPR1.WReg);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_AND)
	{
		mov(TEMP_GPR1.WReg, immS32);
		and_(regR, regA, TEMP_GPR1.WReg);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_OR)
	{
		mov(TEMP_GPR1.WReg, immS32);
		orr(regR, regA, TEMP_GPR1.WReg);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_XOR)
	{
		mov(TEMP_GPR1.WReg, immS32);
		eor(regR, regA, TEMP_GPR1.WReg);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_MULTIPLY_SIGNED)
	{
		mov(TEMP_GPR1.WReg, immS32);
		mul(regR, regA, TEMP_GPR1.WReg);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_LEFT_SHIFT)
	{
		lsl(regR, regA, (uint32)immS32 & 0x1f);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_RIGHT_SHIFT_U)
	{
		lsr(regR, regA, (uint32)immS32 & 0x1f);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_RIGHT_SHIFT_S)
	{
		asr(regR, regA, (uint32)immS32 & 0x1f);
	}
	else
	{
		cemuLog_log(LogType::Recompiler, "PPCRecompilerAArch64Gen_imlInstruction_r_r_s32(): Unsupported operation {:x}", imlInstruction->operation);
		cemu_assert_suspicious();
		return false;
	}
	return true;
}

bool AArch64GenContext_t::r_r_s32_carry(IMLInstruction* imlInstruction)
{
	WReg regR = gpReg<WReg>(imlInstruction->op_r_r_s32_carry.regR);
	WReg regA = gpReg<WReg>(imlInstruction->op_r_r_s32_carry.regA);
	WReg regCarry = gpReg<WReg>(imlInstruction->op_r_r_s32_carry.regCarry);

	sint32 immS32 = imlInstruction->op_r_r_s32_carry.immS32;
	if (imlInstruction->operation == PPCREC_IML_OP_ADD)
	{
		adds_imm(regR, regA, immS32, TEMP_GPR1.WReg);
		cset(regCarry, Cond::CS);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_ADD_WITH_CARRY)
	{
		mov(TEMP_GPR1.WReg, immS32);
		cmp(regCarry, 1);
		adcs(regR, regA, TEMP_GPR1.WReg);
		cset(regCarry, Cond::CS);
	}
	else
	{
		cemu_assert_suspicious();
		return false;
	}

	return true;
}

bool AArch64GenContext_t::r_r_r(IMLInstruction* imlInstruction)
{
	WReg regResult = gpReg<WReg>(imlInstruction->op_r_r_r.regR);
	XReg reg64Result = gpReg<XReg>(imlInstruction->op_r_r_r.regR);
	WReg regOperand1 = gpReg<WReg>(imlInstruction->op_r_r_r.regA);
	WReg regOperand2 = gpReg<WReg>(imlInstruction->op_r_r_r.regB);

	if (imlInstruction->operation == PPCREC_IML_OP_ADD)
	{
		add(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_SUB)
	{
		sub(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_OR)
	{
		orr(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_AND)
	{
		and_(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_XOR)
	{
		eor(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_MULTIPLY_SIGNED)
	{
		mul(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_SLW)
	{
		tst(regOperand2, 32);
		lsl(regResult, regOperand1, regOperand2);
		csel(regResult, regResult, wzr, Cond::EQ);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_SRW)
	{
		tst(regOperand2, 32);
		lsr(regResult, regOperand1, regOperand2);
		csel(regResult, regResult, wzr, Cond::EQ);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_LEFT_ROTATE)
	{
		neg(TEMP_GPR1.WReg, regOperand2);
		ror(regResult, regOperand1, TEMP_GPR1.WReg);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_RIGHT_SHIFT_S)
	{
		asr(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_RIGHT_SHIFT_U)
	{
		lsr(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_LEFT_SHIFT)
	{
		lsl(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_DIVIDE_SIGNED)
	{
		sdiv(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_DIVIDE_UNSIGNED)
	{
		udiv(regResult, regOperand1, regOperand2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_MULTIPLY_HIGH_SIGNED)
	{
		smull(reg64Result, regOperand1, regOperand2);
		lsr(reg64Result, reg64Result, 32);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_MULTIPLY_HIGH_UNSIGNED)
	{
		umull(reg64Result, regOperand1, regOperand2);
		lsr(reg64Result, reg64Result, 32);
	}
	else
	{
		cemuLog_log(LogType::Recompiler, "PPCRecompilerAArch64Gen_imlInstruction_r_r_r(): Unsupported operation {:x}", imlInstruction->operation);
		return false;
	}
	return true;
}

bool AArch64GenContext_t::r_r_r_carry(IMLInstruction* imlInstruction)
{
	WReg regR = gpReg<WReg>(imlInstruction->op_r_r_r_carry.regR);
	WReg regA = gpReg<WReg>(imlInstruction->op_r_r_r_carry.regA);
	WReg regB = gpReg<WReg>(imlInstruction->op_r_r_r_carry.regB);
	WReg regCarry = gpReg<WReg>(imlInstruction->op_r_r_r_carry.regCarry);

	if (imlInstruction->operation == PPCREC_IML_OP_ADD)
	{
		adds(regR, regA, regB);
		cset(regCarry, Cond::CS);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_ADD_WITH_CARRY)
	{
		cmp(regCarry, 1);
		adcs(regR, regA, regB);
		cset(regCarry, Cond::CS);
	}
	else
	{
		cemu_assert_suspicious();
		return false;
	}

	return true;
}

Cond ImlCondToArm64Cond(IMLCondition condition)
{
	switch (condition)
	{
	case IMLCondition::EQ:
		return Cond::EQ;
	case IMLCondition::NEQ:
		return Cond::NE;
	case IMLCondition::UNSIGNED_GT:
		return Cond::HI;
	case IMLCondition::UNSIGNED_LT:
		return Cond::LO;
	case IMLCondition::SIGNED_GT:
		return Cond::GT;
	case IMLCondition::SIGNED_LT:
		return Cond::LT;
	default:
	{
		cemu_assert_suspicious();
		return Cond::EQ;
	}
	}
}

void AArch64GenContext_t::compare(IMLInstruction* imlInstruction)
{
	WReg regR = gpReg<WReg>(imlInstruction->op_compare.regR);
	WReg regA = gpReg<WReg>(imlInstruction->op_compare.regA);
	WReg regB = gpReg<WReg>(imlInstruction->op_compare.regB);
	Cond cond = ImlCondToArm64Cond(imlInstruction->op_compare.cond);
	cmp(regA, regB);
	cset(regR, cond);
}

void AArch64GenContext_t::compare_s32(IMLInstruction* imlInstruction)
{
	WReg regR = gpReg<WReg>(imlInstruction->op_compare.regR);
	WReg regA = gpReg<WReg>(imlInstruction->op_compare.regA);
	sint32 imm = imlInstruction->op_compare_s32.immS32;
	auto cond = ImlCondToArm64Cond(imlInstruction->op_compare.cond);
	cmp_imm(regA, imm, TEMP_GPR1.WReg);
	cset(regR, cond);
}

void AArch64GenContext_t::cjump(IMLInstruction* imlInstruction, IMLSegment* imlSegment)
{
	auto regBool = gpReg<WReg>(imlInstruction->op_conditional_jump.registerBool);
	prepareJump(ConditionalRegJumpInfo{
		.target = imlSegment->nextSegmentBranchTaken,
		.regBool = regBool,
		.mustBeTrue = imlInstruction->op_conditional_jump.mustBeTrue});
}

void AArch64GenContext_t::jump(IMLSegment* imlSegment)
{
	prepareJump(UnconditionalJumpInfo{.target = imlSegment->nextSegmentBranchTaken});
}

void AArch64GenContext_t::conditionalJumpCycleCheck(IMLSegment* imlSegment)
{
	ldr(TEMP_GPR1.WReg, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, remainingCycles)));
	prepareJump(NegativeRegValueJumpInfo{
		.target = imlSegment->nextSegmentBranchTaken,
		.regValue = TEMP_GPR1.WReg,
	});
}

void* ATTR_MS_ABI PPCRecompiler_virtualHLE(PPCInterpreter_t* ppcInterpreter, uint32 hleFuncId)
{
	void* prevRSPTemp = ppcInterpreter->rspTemp;
	if (hleFuncId == 0xFFD0)
	{
		ppcInterpreter->remainingCycles -= 500; // let subtract about 500 cycles for each HLE call
		ppcInterpreter->gpr[3] = 0;
		PPCInterpreter_nextInstruction(ppcInterpreter);
		return PPCInterpreter_getCurrentInstance();
	}
	else
	{
		auto hleCall = PPCInterpreter_getHLECall(hleFuncId);
		cemu_assert(hleCall != nullptr);
		hleCall(ppcInterpreter);
	}
	ppcInterpreter->rspTemp = prevRSPTemp;
	return PPCInterpreter_getCurrentInstance();
}

bool AArch64GenContext_t::macro(IMLInstruction* imlInstruction)
{
	if (imlInstruction->operation == PPCREC_IML_MACRO_B_TO_REG)
	{
		XReg branchDstReg = gpReg<XReg>(imlInstruction->op_macro.paramReg);

		mov(TEMP_GPR1.XReg, offsetof(PPCRecompilerInstanceData_t, ppcRecompilerDirectJumpTable));
		add(TEMP_GPR1.XReg, TEMP_GPR1.XReg, branchDstReg, ShMod::LSL, 1);
		ldr(TEMP_GPR1.XReg, AdrReg(PPC_REC_INSTANCE_REG, TEMP_GPR1.XReg));
		mov(LR.XReg, branchDstReg);
		br(TEMP_GPR1.XReg);
		return true;
	}
	else if (imlInstruction->operation == PPCREC_IML_MACRO_BL)
	{
		uint32 newLR = imlInstruction->op_macro.param + 4;

		mov(TEMP_GPR1.WReg, newLR);
		str(TEMP_GPR1.WReg, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, spr.LR)));

		uint32 newIP = imlInstruction->op_macro.param2;
		uint64 lookupOffset = (uint64)offsetof(PPCRecompilerInstanceData_t, ppcRecompilerDirectJumpTable) + (uint64)newIP * 2ULL;
		mov(TEMP_GPR1.XReg, lookupOffset);
		ldr(TEMP_GPR1.XReg, AdrReg(PPC_REC_INSTANCE_REG, TEMP_GPR1.XReg));
		mov(LR.WReg, newIP);
		br(TEMP_GPR1.XReg);
		return true;
	}
	else if (imlInstruction->operation == PPCREC_IML_MACRO_B_FAR)
	{
		uint32 newIP = imlInstruction->op_macro.param2;
		uint64 lookupOffset = (uint64)offsetof(PPCRecompilerInstanceData_t, ppcRecompilerDirectJumpTable) + (uint64)newIP * 2ULL;
		mov(TEMP_GPR1.XReg, lookupOffset);
		ldr(TEMP_GPR1.XReg, AdrReg(PPC_REC_INSTANCE_REG, TEMP_GPR1.XReg));
		mov(LR.WReg, newIP);
		br(TEMP_GPR1.XReg);
		return true;
	}
	else if (imlInstruction->operation == PPCREC_IML_MACRO_LEAVE)
	{
		uint32 currentInstructionAddress = imlInstruction->op_macro.param;
		mov(TEMP_GPR1.XReg, (uint64)offsetof(PPCRecompilerInstanceData_t, ppcRecompilerDirectJumpTable)); // newIP = 0 special value for recompiler exit
		ldr(TEMP_GPR1.XReg, AdrReg(PPC_REC_INSTANCE_REG, TEMP_GPR1.XReg));
		mov(LR.WReg, currentInstructionAddress);
		br(TEMP_GPR1.XReg);
		return true;
	}
	else if (imlInstruction->operation == PPCREC_IML_MACRO_DEBUGBREAK)
	{
		brk(0xf000);
		return true;
	}
	else if (imlInstruction->operation == PPCREC_IML_MACRO_COUNT_CYCLES)
	{
		uint32 cycleCount = imlInstruction->op_macro.param;
		AdrImm adrCycles = AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, remainingCycles));
		ldr(TEMP_GPR1.WReg, adrCycles);
		sub_imm(TEMP_GPR1.WReg, TEMP_GPR1.WReg, cycleCount, TEMP_GPR2.WReg);
		str(TEMP_GPR1.WReg, adrCycles);
		return true;
	}
	else if (imlInstruction->operation == PPCREC_IML_MACRO_HLE)
	{
		uint32 ppcAddress = imlInstruction->op_macro.param;
		uint32 funcId = imlInstruction->op_macro.param2;
		Label cyclesLeftLabel;

		// update instruction pointer
		mov(TEMP_GPR1.WReg, ppcAddress);
		str(TEMP_GPR1.WReg, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, instructionPointer)));
		// set parameters
		str(x30, AdrPreImm(sp, -16));

		mov(x0, HCPU_REG);
		mov(w1, funcId);
		// call HLE function

		mov(TEMP_GPR1.XReg, (uint64)PPCRecompiler_virtualHLE);
		blr(TEMP_GPR1.XReg);

		mov(HCPU_REG, x0);

		ldr(x30, AdrPostImm(sp, 16));

		// check if cycles where decreased beyond zero, if yes -> leave recompiler
		ldr(TEMP_GPR1.WReg, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, remainingCycles)));
		tbz(TEMP_GPR1.WReg, 31, cyclesLeftLabel); // check if negative

		mov(TEMP_GPR1.XReg, offsetof(PPCRecompilerInstanceData_t, ppcRecompilerDirectJumpTable));
		ldr(TEMP_GPR1.XReg, AdrReg(PPC_REC_INSTANCE_REG, TEMP_GPR1.XReg));
		ldr(LR.WReg, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, instructionPointer)));
		// branch to recompiler exit
		br(TEMP_GPR1.XReg);

		L(cyclesLeftLabel);
		// check if instruction pointer was changed
		// assign new instruction pointer to LR.WReg
		ldr(LR.WReg, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, instructionPointer)));
		mov(TEMP_GPR1.XReg, offsetof(PPCRecompilerInstanceData_t, ppcRecompilerDirectJumpTable));
		add(TEMP_GPR1.XReg, TEMP_GPR1.XReg, LR.XReg, ShMod::LSL, 1);
		ldr(TEMP_GPR1.XReg, AdrReg(PPC_REC_INSTANCE_REG, TEMP_GPR1.XReg));
		// branch to [ppcRecompilerDirectJumpTable + PPCInterpreter_t::instructionPointer * 2]
		br(TEMP_GPR1.XReg);
		return true;
	}
	else
	{
		cemuLog_log(LogType::Recompiler, "Unknown recompiler macro operation %d\n", imlInstruction->operation);
		cemu_assert_suspicious();
	}
	return false;
}

bool AArch64GenContext_t::load(IMLInstruction* imlInstruction, bool indexed)
{
	cemu_assert_debug(imlInstruction->op_storeLoad.registerData.GetRegFormat() == IMLRegFormat::I32);
	cemu_assert_debug(imlInstruction->op_storeLoad.registerMem.GetRegFormat() == IMLRegFormat::I32);
	if (indexed)
		cemu_assert_debug(imlInstruction->op_storeLoad.registerMem2.GetRegFormat() == IMLRegFormat::I32);

	sint32 memOffset = imlInstruction->op_storeLoad.immS32;
	bool signExtend = imlInstruction->op_storeLoad.flags2.signExtend;
	bool switchEndian = imlInstruction->op_storeLoad.flags2.swapEndian;
	WReg memReg = gpReg<WReg>(imlInstruction->op_storeLoad.registerMem);
	WReg dataReg = gpReg<WReg>(imlInstruction->op_storeLoad.registerData);

	add_imm(TEMP_GPR1.WReg, memReg, memOffset, TEMP_GPR1.WReg);
	if (indexed)
		add(TEMP_GPR1.WReg, TEMP_GPR1.WReg, gpReg<WReg>(imlInstruction->op_storeLoad.registerMem2));

	auto adr = AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW);
	if (imlInstruction->op_storeLoad.copyWidth == 32)
	{
		ldr(dataReg, adr);
		if (switchEndian)
			rev(dataReg, dataReg);
	}
	else if (imlInstruction->op_storeLoad.copyWidth == 16)
	{
		if (switchEndian)
		{
			ldrh(dataReg, adr);
			rev(dataReg, dataReg);
			if (signExtend)
				asr(dataReg, dataReg, 16);
			else
				lsr(dataReg, dataReg, 16);
		}
		else
		{
			if (signExtend)
				ldrsh(dataReg, adr);
			else
				ldrh(dataReg, adr);
		}
	}
	else if (imlInstruction->op_storeLoad.copyWidth == 8)
	{
		if (signExtend)
			ldrsb(dataReg, adr);
		else
			ldrb(dataReg, adr);
	}
	else
	{
		return false;
	}
	return true;
}

bool AArch64GenContext_t::store(IMLInstruction* imlInstruction, bool indexed)
{
	cemu_assert_debug(imlInstruction->op_storeLoad.registerData.GetRegFormat() == IMLRegFormat::I32);
	cemu_assert_debug(imlInstruction->op_storeLoad.registerMem.GetRegFormat() == IMLRegFormat::I32);
	if (indexed)
		cemu_assert_debug(imlInstruction->op_storeLoad.registerMem2.GetRegFormat() == IMLRegFormat::I32);

	WReg dataReg = gpReg<WReg>(imlInstruction->op_storeLoad.registerData);
	WReg memReg = gpReg<WReg>(imlInstruction->op_storeLoad.registerMem);
	sint32 memOffset = imlInstruction->op_storeLoad.immS32;
	bool swapEndian = imlInstruction->op_storeLoad.flags2.swapEndian;

	add_imm(TEMP_GPR1.WReg, memReg, memOffset, TEMP_GPR1.WReg);
	if (indexed)
		add(TEMP_GPR1.WReg, TEMP_GPR1.WReg, gpReg<WReg>(imlInstruction->op_storeLoad.registerMem2));
	AdrExt adr = AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW);
	if (imlInstruction->op_storeLoad.copyWidth == 32)
	{
		if (swapEndian)
		{
			rev(TEMP_GPR2.WReg, dataReg);
			str(TEMP_GPR2.WReg, adr);
		}
		else
		{
			str(dataReg, adr);
		}
	}
	else if (imlInstruction->op_storeLoad.copyWidth == 16)
	{
		if (swapEndian)
		{
			rev(TEMP_GPR2.WReg, dataReg);
			lsr(TEMP_GPR2.WReg, TEMP_GPR2.WReg, 16);
			strh(TEMP_GPR2.WReg, adr);
		}
		else
		{
			strh(dataReg, adr);
		}
	}
	else if (imlInstruction->op_storeLoad.copyWidth == 8)
	{
		strb(dataReg, adr);
	}
	else
	{
		return false;
	}
	return true;
}

void AArch64GenContext_t::atomic_cmp_store(IMLInstruction* imlInstruction)
{
	WReg outReg = gpReg<WReg>(imlInstruction->op_atomic_compare_store.regBoolOut);
	WReg eaReg = gpReg<WReg>(imlInstruction->op_atomic_compare_store.regEA);
	WReg valReg = gpReg<WReg>(imlInstruction->op_atomic_compare_store.regWriteValue);
	WReg cmpValReg = gpReg<WReg>(imlInstruction->op_atomic_compare_store.regCompareValue);

	if (s_cpu.isAtomicSupported())
	{
		mov(TEMP_GPR2.WReg, cmpValReg);
		add(TEMP_GPR1.XReg, MEM_BASE_REG, eaReg, ExtMod::UXTW);
		casal(TEMP_GPR2.WReg, valReg, AdrNoOfs(TEMP_GPR1.XReg));
		cmp(TEMP_GPR2.WReg, cmpValReg);
		cset(outReg, Cond::EQ);
	}
	else
	{
		Label notEqual;
		Label storeFailed;

		add(TEMP_GPR1.XReg, MEM_BASE_REG, eaReg, ExtMod::UXTW);
		L(storeFailed);
		ldaxr(TEMP_GPR2.WReg, AdrNoOfs(TEMP_GPR1.XReg));
		cmp(TEMP_GPR2.WReg, cmpValReg);
		bne(notEqual);
		stlxr(TEMP_GPR2.WReg, valReg, AdrNoOfs(TEMP_GPR1.XReg));
		cbnz(TEMP_GPR2.WReg, storeFailed);

		L(notEqual);
		cset(outReg, Cond::EQ);
	}
}

void AArch64GenContext_t::gqr_generateScaleCode(const VReg& resReg, const VReg& dataReg, bool isLoad, bool scalePS1, const IMLReg& registerGQR)
{
	auto gqrReg = gpReg<WReg>(registerGQR);
	// load GQR & extract scale field and multiply by 16 to get array offset
	lsr(TEMP_GPR1.WReg, gqrReg, (isLoad ? 16 : 0) + 8 - 4);
	and_(TEMP_GPR1.WReg, TEMP_GPR1.WReg, (0x3F << 4));
	// multiply dataReg by scale
	if (isLoad)
	{
		if (scalePS1)
			mov(TEMP_GPR2.XReg, offsetof(PPCRecompilerInstanceData_t, _psq_ld_scale_ps0_ps1));
		else
			mov(TEMP_GPR2.XReg, offsetof(PPCRecompilerInstanceData_t, _psq_ld_scale_ps0_1));
	}
	else
	{
		if (scalePS1)
			mov(TEMP_GPR2.XReg, offsetof(PPCRecompilerInstanceData_t, _psq_st_scale_ps0_ps1));
		else
			mov(TEMP_GPR2.XReg, offsetof(PPCRecompilerInstanceData_t, _psq_st_scale_ps0_1));
	}
	add(TEMP_GPR1.XReg, TEMP_GPR1.XReg, TEMP_GPR2.XReg);
	cemu_assert_debug(dataReg.getIdx() != TEMP_FPR1.index);
	ldr(TEMP_FPR1.QReg, AdrReg(PPC_REC_INSTANCE_REG, TEMP_GPR1.XReg));
	fmul(resReg.d2, dataReg.d2, TEMP_FPR1.VReg.d2);
}

// generate code for PSQ load for a particular type
// if scaleGQR is -1 then a scale of 1.0 is assumed (no scale)
void AArch64GenContext_t::psq_load(uint8 mode, VReg& dataVReg, WReg& memReg, WReg& indexReg, sint32 memImmS32, bool indexed, const IMLReg& registerGQR)
{
	DReg dataDReg{dataVReg.getIdx()};
	BReg dataBReg{dataVReg.getIdx()};
	SReg dataSReg{dataVReg.getIdx()};
	if (mode == PPCREC_FPR_LD_MODE_PSQ_FLOAT_PS0_PS1)
	{
		add_imm(TEMP_GPR1.WReg, memReg, memImmS32, TEMP_GPR1.WReg);
		if (indexed)
			cemu_assert_suspicious();
		ldr(dataDReg, AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW));
		rev32(dataVReg.b8, dataVReg.b8);
		fcvtl(dataVReg.d2, dataVReg.s2);
		// note: floats are not scaled
	}
	else if (mode == PPCREC_FPR_LD_MODE_PSQ_FLOAT_PS0)
	{
		add_imm(TEMP_GPR1.WReg, memReg, memImmS32, TEMP_GPR1.WReg);
		if (indexed)
			cemu_assert_suspicious();
		ldr(TEMP_GPR2.WReg, AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW));
		rev(TEMP_GPR2.WReg, TEMP_GPR2.WReg);
		fmov(dataVReg.d2, 1.0);
		fmov(TEMP_FPR1.SReg, TEMP_GPR2.WReg);
		fcvt(TEMP_FPR1.DReg, TEMP_FPR1.SReg);
		mov(dataVReg.d[0], TEMP_FPR1.VReg.d[0]);
		// note: floats are not scaled
	}
	else
	{
		if (indexed)
			cemu_assert_suspicious();
		bool loadPS1 = false;
		if (mode == PPCREC_FPR_LD_MODE_PSQ_S16_PS0_PS1 || mode == PPCREC_FPR_LD_MODE_PSQ_U16_PS0_PS1)
		{
			loadPS1 = true;
			add_imm(TEMP_GPR1.WReg, memReg, memImmS32, TEMP_GPR1.WReg);
			add(TEMP_GPR1.XReg, MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW);
			ldrh(TEMP_GPR2.WReg, AdrNoOfs(TEMP_GPR1.XReg));
			ldrh(TEMP_GPR1.WReg, AdrImm(TEMP_GPR1.XReg, 2));
			rev(TEMP_GPR1.WReg, TEMP_GPR1.WReg);
			rev(TEMP_GPR2.WReg, TEMP_GPR2.WReg);
			if (mode == PPCREC_FPR_LD_MODE_PSQ_S16_PS0_PS1)
			{
				asr(TEMP_GPR1.WReg, TEMP_GPR1.WReg, 16);
				asr(TEMP_GPR2.WReg, TEMP_GPR2.WReg, 16);
				scvtf(TEMP_FPR1.DReg, TEMP_GPR1.WReg);
				scvtf(dataDReg, TEMP_GPR2.WReg);
			}
			else
			{
				lsr(TEMP_GPR1.WReg, TEMP_GPR1.WReg, 16);
				lsr(TEMP_GPR2.WReg, TEMP_GPR2.WReg, 16);
				ucvtf(TEMP_FPR1.DReg, TEMP_GPR1.WReg);
				ucvtf(dataDReg, TEMP_GPR2.WReg);
			}
			mov(dataVReg.d[1], TEMP_FPR1.VReg.d[0]);
		}
		else if (mode == PPCREC_FPR_LD_MODE_PSQ_S16_PS0 || mode == PPCREC_FPR_LD_MODE_PSQ_U16_PS0)
		{
			add_imm(TEMP_GPR1.WReg, memReg, memImmS32, TEMP_GPR1.WReg);
			auto adr = AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW);
			ldrh(TEMP_GPR1.WReg, adr);
			rev(TEMP_GPR1.WReg, TEMP_GPR1.WReg);
			if (mode == PPCREC_FPR_LD_MODE_PSQ_S16_PS0)
			{
				asr(TEMP_GPR1.WReg, TEMP_GPR1.WReg, 16);
				scvtf(TEMP_FPR1.DReg, TEMP_GPR1.WReg);
			}
			else
			{
				lsr(TEMP_GPR1.WReg, TEMP_GPR1.WReg, 16);
				ucvtf(TEMP_FPR1.DReg, TEMP_GPR1.WReg);
			}
			fmov(dataVReg.d2, 1.0);
			mov(dataVReg.d[0], TEMP_FPR1.VReg.d[0]);
		}
		else if (mode == PPCREC_FPR_LD_MODE_PSQ_S8_PS0_PS1 || mode == PPCREC_FPR_LD_MODE_PSQ_U8_PS0_PS1)
		{
			loadPS1 = true;
			add_imm(TEMP_GPR1.WReg, memReg, memImmS32, TEMP_GPR1.WReg);
			add(TEMP_GPR1.XReg, MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW);
			if (mode == PPCREC_FPR_LD_MODE_PSQ_S8_PS0_PS1)
			{
				ldrsb(TEMP_GPR2.WReg, AdrNoOfs(TEMP_GPR1.XReg));
				ldrsb(TEMP_GPR1.WReg, AdrImm(TEMP_GPR1.XReg, 1));
				scvtf(dataDReg, TEMP_GPR2.WReg);
				scvtf(TEMP_FPR1.DReg, TEMP_GPR1.WReg);
			}
			else
			{
				ldr(dataBReg, AdrNoOfs(TEMP_GPR1.XReg));
				ldr(TEMP_FPR1.BReg, AdrImm(TEMP_GPR1.XReg, 1));
				ucvtf(dataDReg, dataDReg);
				ucvtf(TEMP_FPR1.DReg, TEMP_FPR1.DReg);
			}
			mov(dataVReg.d[1], TEMP_FPR1.VReg.d[0]);
		}
		else if (mode == PPCREC_FPR_LD_MODE_PSQ_S8_PS0 || mode == PPCREC_FPR_LD_MODE_PSQ_U8_PS0)
		{
			add_imm(TEMP_GPR1.WReg, memReg, memImmS32, TEMP_GPR1.WReg);
			auto adr = AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW);
			if (mode == PPCREC_FPR_LD_MODE_PSQ_S8_PS0)
			{
				ldrsb(TEMP_GPR1.WReg, adr);
				scvtf(TEMP_FPR1.DReg, TEMP_GPR1.WReg);
			}
			else
			{
				ldr(TEMP_FPR1.BReg, adr);
				ucvtf(TEMP_FPR1.DReg, TEMP_FPR1.DReg);
			}
			fmov(dataVReg.d2, 1.0);
			mov(dataVReg.d[0], TEMP_FPR1.VReg.d[0]);
		}
		// scale
		if (registerGQR.IsValid())
			gqr_generateScaleCode(dataVReg, dataVReg, true, loadPS1, registerGQR);
	}
}

void AArch64GenContext_t::psq_load_generic(uint8 mode, VReg& dataReg, WReg& memReg, WReg& indexReg, sint32 memImmS32, bool indexed, const IMLReg& registerGQR)
{
	bool loadPS1 = (mode == PPCREC_FPR_LD_MODE_PSQ_GENERIC_PS0_PS1);
	Label u8FormatLabel, u16FormatLabel, s8FormatLabel, s16FormatLabel, casesEndLabel;

	// load GQR & extract load type field
	lsr(TEMP_GPR1.WReg, gpReg<WReg>(registerGQR), 16);
	and_(TEMP_GPR1.WReg, TEMP_GPR1.WReg, 7);

	// jump cases
	cmp(TEMP_GPR1.WReg, 4); // type 4 -> u8
	beq(u8FormatLabel);

	cmp(TEMP_GPR1.WReg, 5); // type 5 -> u16
	beq(u16FormatLabel);

	cmp(TEMP_GPR1.WReg, 6); // type 6 -> s8
	beq(s8FormatLabel);

	cmp(TEMP_GPR1.WReg, 7); // type 7 -> s16
	beq(s16FormatLabel);

	// default case -> float

	// generate cases
	psq_load(loadPS1 ? PPCREC_FPR_LD_MODE_PSQ_FLOAT_PS0_PS1 : PPCREC_FPR_LD_MODE_PSQ_FLOAT_PS0, dataReg, memReg, indexReg, memImmS32, indexed, registerGQR);
	b(casesEndLabel);

	L(u16FormatLabel);
	psq_load(loadPS1 ? PPCREC_FPR_LD_MODE_PSQ_U16_PS0_PS1 : PPCREC_FPR_LD_MODE_PSQ_U16_PS0, dataReg, memReg, indexReg, memImmS32, indexed, registerGQR);
	b(casesEndLabel);

	L(s16FormatLabel);
	psq_load(loadPS1 ? PPCREC_FPR_LD_MODE_PSQ_S16_PS0_PS1 : PPCREC_FPR_LD_MODE_PSQ_S16_PS0, dataReg, memReg, indexReg, memImmS32, indexed, registerGQR);
	b(casesEndLabel);

	L(u8FormatLabel);
	psq_load(loadPS1 ? PPCREC_FPR_LD_MODE_PSQ_U8_PS0_PS1 : PPCREC_FPR_LD_MODE_PSQ_U8_PS0, dataReg, memReg, indexReg, memImmS32, indexed, registerGQR);
	b(casesEndLabel);

	L(s8FormatLabel);
	psq_load(loadPS1 ? PPCREC_FPR_LD_MODE_PSQ_S8_PS0_PS1 : PPCREC_FPR_LD_MODE_PSQ_S8_PS0, dataReg, memReg, indexReg, memImmS32, indexed, registerGQR);

	L(casesEndLabel);
}

bool AArch64GenContext_t::fpr_load(IMLInstruction* imlInstruction, bool indexed)
{
	const IMLReg& dataReg = imlInstruction->op_storeLoad.registerData;
	VReg dataVReg = fpReg<VReg>(dataReg);
	SReg dataSReg = fpReg<SReg>(dataReg);
	DReg dataDReg = fpReg<DReg>(dataReg);
	WReg realRegisterMem = gpReg<WReg>(imlInstruction->op_storeLoad.registerMem);
	WReg realRegisterMem2 = indexed ? gpReg<WReg>(imlInstruction->op_storeLoad.registerMem2) : wzr;
	sint32 adrOffset = imlInstruction->op_storeLoad.immS32;
	uint8 mode = imlInstruction->op_storeLoad.mode;

	if (mode == PPCREC_FPR_LD_MODE_SINGLE_INTO_PS0_PS1)
	{
		add_imm(TEMP_GPR1.WReg, realRegisterMem, adrOffset, TEMP_GPR1.WReg);
		if (indexed)
			add(TEMP_GPR1.WReg, TEMP_GPR1.WReg, realRegisterMem2);
		ldr(TEMP_GPR1.WReg, AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW));
		rev(TEMP_GPR1.WReg, TEMP_GPR1.WReg);
		fmov(dataSReg, TEMP_GPR1.WReg);

		if (imlInstruction->op_storeLoad.flags2.notExpanded)
		{
			// leave value as single
		}
		else
		{
			fcvt(dataDReg, dataSReg);
			dup(dataVReg.d2, dataVReg.d[0]);
		}
	}
	else if (mode == PPCREC_FPR_LD_MODE_DOUBLE_INTO_PS0)
	{
		add_imm(TEMP_GPR1.WReg, realRegisterMem, adrOffset, TEMP_GPR1.WReg);
		if (indexed)
			add(TEMP_GPR1.WReg, TEMP_GPR1.WReg, realRegisterMem2);
		ldr(TEMP_GPR1.XReg, AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW));
		rev(TEMP_GPR1.XReg, TEMP_GPR1.XReg);
		mov(dataVReg.d[0], TEMP_GPR1.XReg);
	}
	else if (mode == PPCREC_FPR_LD_MODE_PSQ_FLOAT_PS0_PS1 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_FLOAT_PS0 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_S16_PS0 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_S16_PS0_PS1 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_U16_PS0 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_U16_PS0_PS1 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_S8_PS0 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_S8_PS0_PS1 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_U8_PS0 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_U8_PS0_PS1)
	{
		psq_load(mode, dataVReg, realRegisterMem, realRegisterMem2, imlInstruction->op_storeLoad.immS32, indexed);
	}
	else if (mode == PPCREC_FPR_LD_MODE_PSQ_GENERIC_PS0_PS1 ||
			 mode == PPCREC_FPR_LD_MODE_PSQ_GENERIC_PS0)
	{
		psq_load_generic(mode, dataVReg, realRegisterMem, realRegisterMem2, imlInstruction->op_storeLoad.immS32, indexed, imlInstruction->op_storeLoad.registerGQR);
	}
	else
	{
		return false;
	}
	return true;
}

void AArch64GenContext_t::psq_store(uint8 mode, const IMLReg& dataReg, WReg& memReg, WReg& indexReg, sint32 memOffset, bool indexed, const IMLReg& registerGQR)
{
	auto dataVReg = fpReg<VReg>(dataReg);
	auto dataDReg = fpReg<DReg>(dataReg);

	bool storePS1 = (mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0_PS1 ||
					 mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0_PS1 ||
					 mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0_PS1 ||
					 mode == PPCREC_FPR_ST_MODE_PSQ_U16_PS0_PS1 ||
					 mode == PPCREC_FPR_ST_MODE_PSQ_S16_PS0_PS1);
	bool isFloat = mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0 || mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0_PS1;

	if (registerGQR.IsValid() && !isFloat)
	{
		// apply scale
		gqr_generateScaleCode(TEMP_FPR1.VReg, dataVReg, false, storePS1, registerGQR);
		dataVReg = TEMP_FPR1.VReg;
		dataDReg = TEMP_FPR1.DReg;
	}
	if (mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0)
	{
		add_imm(TEMP_GPR1.WReg, memReg, memOffset, TEMP_GPR1.WReg);
		if (indexed)
			add(TEMP_GPR1.WReg, TEMP_GPR1.WReg, indexReg);
		fcvt(TEMP_FPR1.SReg, dataDReg);
		rev32(TEMP_FPR1.VReg.b8, TEMP_FPR1.VReg.b8);
		str(TEMP_FPR1.SReg, AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW));
	}
	else if (mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0_PS1)
	{
		add_imm(TEMP_GPR1.WReg, memReg, memOffset, TEMP_GPR1.WReg);
		if (indexed)
			add(TEMP_GPR1.WReg, TEMP_GPR1.WReg, indexReg);
		fcvtn(TEMP_FPR1.VReg.s2, dataVReg.d2);
		rev32(TEMP_FPR1.VReg.b8, TEMP_FPR1.VReg.b8);
		str(TEMP_FPR1.DReg, AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW));
	}
	else
	{
		// store as integer
		if (indexed)
			cemu_assert_suspicious(); // unsupported

		if (mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0 || mode == PPCREC_FPR_ST_MODE_PSQ_U16_PS0)
		{
			fcvtzs(TEMP_GPR1.WReg, dataDReg);
			uint64 maxVal = mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0 ? 255 : 65535;
			// clamp
			mov(TEMP_GPR2.WReg, maxVal);
			bic(TEMP_GPR1.WReg, TEMP_GPR1.WReg, TEMP_GPR1.WReg, ShMod::ASR, 31);
			cmp(TEMP_GPR1.WReg, TEMP_GPR2.WReg);
			csel(TEMP_GPR2.WReg, TEMP_GPR1.WReg, TEMP_GPR2.WReg, Cond::LT);
			// write to memory
			add_imm(TEMP_GPR1.WReg, memReg, memOffset, TEMP_GPR1.WReg);
			auto adr = AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW);
			if (mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0)
			{
				strb(TEMP_GPR2.WReg, AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW));
			}
			else
			{
				rev(TEMP_GPR2.WReg, TEMP_GPR2.WReg);
				lsr(TEMP_GPR2.WReg, TEMP_GPR2.WReg, 16);
				strh(TEMP_GPR2.WReg, AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW));
			}
		}
		else if (mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0 || mode == PPCREC_FPR_ST_MODE_PSQ_S16_PS0)
		{
			fcvtzs(TEMP_GPR1.XReg, dataDReg);
			sint32 max;
			if (mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0)
			{
				cmn(TEMP_GPR1.XReg, 128);
				mov(TEMP_GPR2.XReg, -128);
				max = 127;
			}
			else
			{
				cmn(TEMP_GPR1.XReg, 8, 12);
				mov(TEMP_GPR2.XReg, -32768);
				max = 32767;
			}
			// clamp
			csel(TEMP_GPR1.XReg, TEMP_GPR1.XReg, TEMP_GPR2.XReg, Cond::GE);
			mov(TEMP_GPR2.XReg, max);
			cmp(TEMP_GPR1.XReg, TEMP_GPR2.XReg);
			csel(TEMP_GPR1.XReg, TEMP_GPR1.XReg, TEMP_GPR2.XReg, Cond::LE);
			add_imm(TEMP_GPR2.WReg, memReg, memOffset, TEMP_GPR2.WReg);
			auto adr = AdrExt(MEM_BASE_REG, TEMP_GPR2.WReg, ExtMod::UXTW);
			// write to memory
			if (mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0)
			{
				strb(TEMP_GPR1.WReg, adr);
			}
			else
			{
				rev(TEMP_GPR1.WReg, TEMP_GPR1.WReg);
				lsr(TEMP_GPR1.WReg, TEMP_GPR1.WReg, 16);
				strh(TEMP_GPR1.WReg, adr);
			}
		}
		else if (mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0_PS1 || mode == PPCREC_FPR_ST_MODE_PSQ_U16_PS0_PS1)
		{
			fcvtzs(TEMP_FPR1.VReg.d2, dataVReg.d2);
			// clamp
			uint32 max = mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0_PS1 ? 255 : 65535;
			movi(TEMP_FPR3.VReg.d2, max);
			cmgt(TEMP_FPR2.VReg.d2, TEMP_FPR1.VReg.d2, 0);
			and_(TEMP_FPR1.VReg.b16, TEMP_FPR1.VReg.b16, TEMP_FPR2.VReg.b16);
			cmgt(TEMP_FPR2.VReg.d2, TEMP_FPR3.VReg.d2, TEMP_FPR1.VReg.d2);
			bif(TEMP_FPR1.VReg.b16, TEMP_FPR3.VReg.b16, TEMP_FPR2.VReg.b16);
			// write to memory
			add_imm(TEMP_GPR1.WReg, memReg, memOffset, TEMP_GPR1.WReg);
			auto adr = AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW);

			if (mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0_PS1)
			{
				mov(TEMP_FPR1.VReg.b[1], TEMP_FPR1.VReg.b[8]);
				str(TEMP_FPR1.HReg, adr);
			}
			else
			{
				mov(TEMP_FPR1.VReg.h[1], TEMP_FPR1.VReg.h[4]);
				// endian swap
				rev16(TEMP_FPR1.VReg.b8, TEMP_FPR1.VReg.b8);
				// write to memory
				str(TEMP_FPR1.SReg, adr);
			}
		}
		else if (mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0_PS1 || mode == PPCREC_FPR_ST_MODE_PSQ_S16_PS0_PS1)
		{
			fcvtzs(TEMP_FPR2.VReg.d2, dataVReg.d2);
			// clamp
			sint32 min, max;
			if (mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0_PS1)
			{
				min = -128;
				max = 127;
			}
			else
			{
				min = -32768;
				max = 32767;
			}
			mov(TEMP_GPR1.XReg, min);
			dup(TEMP_FPR1.VReg.d2, TEMP_GPR1.XReg);
			mov(TEMP_GPR1.WReg, max);
			cmgt(TEMP_FPR3.VReg.d2, TEMP_FPR2.VReg.d2, TEMP_FPR1.VReg.d2);
			bit(TEMP_FPR1.VReg.b16, TEMP_FPR2.VReg.b16, TEMP_FPR3.VReg.b16);
			dup(TEMP_FPR2.VReg.d2, TEMP_GPR1.XReg);
			cmgt(TEMP_FPR3.VReg.d2, TEMP_FPR2.VReg.d2, TEMP_FPR1.VReg.d2);
			bif(TEMP_FPR1.VReg.b16, TEMP_FPR2.VReg.b16, TEMP_FPR3.VReg.b16);

			// write to memory
			add_imm(TEMP_GPR1.WReg, memReg, memOffset, TEMP_GPR1.WReg);
			auto adr = AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW);

			if (mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0_PS1)
			{
				mov(TEMP_FPR1.VReg.b[1], TEMP_FPR1.VReg.b[8]);
				str(TEMP_FPR1.HReg, adr);
			}
			else
			{
				mov(TEMP_FPR1.VReg.h[1], TEMP_FPR1.VReg.h[4]);
				// endian swap
				rev16(TEMP_FPR1.VReg.b8, TEMP_FPR1.VReg.b8);
				str(TEMP_FPR1.SReg, adr);
			}
		}
		else
		{
			cemu_assert_suspicious();
			return;
		}
	}
}

void AArch64GenContext_t::psq_store_generic(uint8 mode, const IMLReg& dataReg, WReg& memReg, WReg& indexReg, sint32 memOffset, bool indexed, const IMLReg& registerGQR)
{
	bool storePS1 = (mode == PPCREC_FPR_ST_MODE_PSQ_GENERIC_PS0_PS1);
	Label u8FormatLabel, u16FormatLabel, s8FormatLabel, s16FormatLabel, casesEndLabel;
	// load GQR & extract store type field
	and_(TEMP_GPR1.WReg, gpReg<WReg>(registerGQR), 7);

	// jump cases
	cmp(TEMP_GPR1.WReg, 4); // type 4 -> u8
	beq(u8FormatLabel);

	cmp(TEMP_GPR1.WReg, 5); // type 5 -> u16
	beq(u16FormatLabel);

	cmp(TEMP_GPR1.WReg, 6); // type 6 -> s8
	beq(s8FormatLabel);

	cmp(TEMP_GPR1.WReg, 7); // type 7 -> s16
	beq(s16FormatLabel);

	// default case -> float

	// generate cases
	psq_store(storePS1 ? PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0_PS1 : PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0, dataReg, memReg, indexReg, memOffset, indexed, registerGQR);
	b(casesEndLabel);

	L(u16FormatLabel);
	psq_store(storePS1 ? PPCREC_FPR_ST_MODE_PSQ_U16_PS0_PS1 : PPCREC_FPR_ST_MODE_PSQ_U16_PS0, dataReg, memReg, indexReg, memOffset, indexed, registerGQR);
	b(casesEndLabel);

	L(s16FormatLabel);
	psq_store(storePS1 ? PPCREC_FPR_ST_MODE_PSQ_S16_PS0_PS1 : PPCREC_FPR_ST_MODE_PSQ_S16_PS0, dataReg, memReg, indexReg, memOffset, indexed, registerGQR);
	b(casesEndLabel);

	L(u8FormatLabel);
	psq_store(storePS1 ? PPCREC_FPR_ST_MODE_PSQ_U8_PS0_PS1 : PPCREC_FPR_ST_MODE_PSQ_U8_PS0, dataReg, memReg, indexReg, memOffset, indexed, registerGQR);
	b(casesEndLabel);

	L(s8FormatLabel);
	psq_store(storePS1 ? PPCREC_FPR_ST_MODE_PSQ_S8_PS0_PS1 : PPCREC_FPR_ST_MODE_PSQ_S8_PS0, dataReg, memReg, indexReg, memOffset, indexed, registerGQR);

	L(casesEndLabel);
}

// store to memory
bool AArch64GenContext_t::fpr_store(IMLInstruction* imlInstruction, bool indexed)
{
	const IMLReg& dataImlReg = imlInstruction->op_storeLoad.registerData;
	VReg dataVReg = fpReg<VReg>(dataImlReg);
	DReg dataDReg = fpReg<DReg>(dataImlReg);
	WReg memReg = gpReg<WReg>(imlInstruction->op_storeLoad.registerMem);
	WReg indexReg = indexed ? gpReg<WReg>(imlInstruction->op_storeLoad.registerMem2) : wzr;
	sint32 memOffset = imlInstruction->op_storeLoad.immS32;
	uint8 mode = imlInstruction->op_storeLoad.mode;

	if (mode == PPCREC_FPR_ST_MODE_SINGLE_FROM_PS0)
	{
		add_imm(TEMP_GPR1.WReg, memReg, memOffset, TEMP_GPR1.WReg);
		if (indexed)
			add(TEMP_GPR1.WReg, TEMP_GPR1.WReg, indexReg);
		auto adr = AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW);
		if (imlInstruction->op_storeLoad.flags2.notExpanded)
		{
			// value is already in single format
			mov(TEMP_GPR2.WReg, dataVReg.s[0]);
		}
		else
		{
			fcvt(TEMP_FPR1.SReg, dataDReg);
			fmov(TEMP_GPR2.WReg, TEMP_FPR1.SReg);
		}
		rev(TEMP_GPR2.WReg, TEMP_GPR2.WReg);
		str(TEMP_GPR2.WReg, adr);
	}
	else if (mode == PPCREC_FPR_ST_MODE_DOUBLE_FROM_PS0)
	{
		add_imm(TEMP_GPR1.WReg, memReg, memOffset, TEMP_GPR1.WReg);
		if (indexed)
			add(TEMP_GPR1.WReg, TEMP_GPR1.WReg, indexReg);
		mov(TEMP_GPR2.XReg, dataVReg.d[0]);
		rev(TEMP_GPR2.XReg, TEMP_GPR2.XReg);
		str(TEMP_GPR2.XReg, AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW));
	}
	else if (mode == PPCREC_FPR_ST_MODE_UI32_FROM_PS0)
	{
		add_imm(TEMP_GPR1.WReg, memReg, memOffset, TEMP_GPR1.WReg);
		if (indexed)
			add(TEMP_GPR1.WReg, TEMP_GPR1.WReg, indexReg);
		mov(TEMP_GPR2.WReg, dataVReg.s[0]);
		rev(TEMP_GPR2.WReg, TEMP_GPR2.WReg);
		str(TEMP_GPR2.WReg, AdrExt(MEM_BASE_REG, TEMP_GPR1.WReg, ExtMod::UXTW));
	}
	else if (mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0_PS1 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_FLOAT_PS0 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_S8_PS0_PS1 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_U8_PS0_PS1 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_S16_PS0 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_S16_PS0_PS1 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_U16_PS0 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_U16_PS0_PS1)
	{
		cemu_assert_debug(imlInstruction->op_storeLoad.flags2.notExpanded == false);
		psq_store(mode, dataImlReg, memReg, indexReg, imlInstruction->op_storeLoad.immS32, indexed);
	}
	else if (mode == PPCREC_FPR_ST_MODE_PSQ_GENERIC_PS0_PS1 ||
			 mode == PPCREC_FPR_ST_MODE_PSQ_GENERIC_PS0)
	{
		psq_store_generic(mode, dataImlReg, memReg, indexReg, imlInstruction->op_storeLoad.immS32, indexed, imlInstruction->op_storeLoad.registerGQR);
	}
	else
	{
		cemu_assert_suspicious();
		cemuLog_log(LogType::Recompiler, "PPCRecompilerAArch64Gen_imlInstruction_fpr_store(): Unsupported mode %d\n", mode);
		return false;
	}
	return true;
}

// FPR op FPR
void AArch64GenContext_t::fpr_r_r(IMLInstruction* imlInstruction)
{
	VReg regRVReg = fpReg<VReg>(imlInstruction->op_fpr_r_r.regR);
	VReg regAVReg = fpReg<VReg>(imlInstruction->op_fpr_r_r.regA);
	DReg regADReg = fpReg<DReg>(imlInstruction->op_fpr_r_r.regA);

	if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_BOTTOM_TO_BOTTOM_AND_TOP)
	{
		dup(regRVReg.d2, regAVReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_TOP_TO_BOTTOM_AND_TOP)
	{
		dup(regRVReg.d2, regAVReg.d[1]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_BOTTOM_TO_BOTTOM)
	{
		mov(regRVReg.d[0], regAVReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_BOTTOM_TO_TOP)
	{
		mov(regRVReg.d[1], regAVReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_BOTTOM_AND_TOP_SWAPPED)
	{
		ext(regRVReg.b16, regAVReg.b16, regAVReg.b16, 8);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_TOP_TO_TOP)
	{
		mov(regRVReg.d[1], regAVReg.d[1]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_COPY_TOP_TO_BOTTOM)
	{
		mov(regRVReg.d[0], regAVReg.d[1]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_MULTIPLY_BOTTOM)
	{
		mov(TEMP_FPR1.VReg.b16, regAVReg.b16);
		fmul(TEMP_FPR1.VReg.d2, regRVReg.d2, TEMP_FPR1.VReg.d2);
		mov(regRVReg.d[0], TEMP_FPR1.VReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_MULTIPLY_PAIR)
	{
		fmul(regRVReg.d2, regRVReg.d2, regAVReg.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_DIVIDE_BOTTOM)
	{
		mov(TEMP_FPR1.VReg.b16, regAVReg.b16);
		fdiv(TEMP_FPR1.VReg.d2, regRVReg.d2, TEMP_FPR1.VReg.d2);
		mov(regRVReg.d[0], TEMP_FPR1.VReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_DIVIDE_PAIR)
	{
		fdiv(regRVReg.d2, regRVReg.d2, regAVReg.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ADD_BOTTOM)
	{
		mov(TEMP_FPR1.VReg.b16, regAVReg.b16);
		fadd(TEMP_FPR1.VReg.d2, regRVReg.d2, TEMP_FPR1.VReg.d2);
		mov(regRVReg.d[0], TEMP_FPR1.VReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ADD_PAIR)
	{
		fadd(regRVReg.d2, regRVReg.d2, regAVReg.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SUB_PAIR)
	{
		fsub(regRVReg.d2, regRVReg.d2, regAVReg.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SUB_BOTTOM)
	{
		mov(TEMP_FPR1.VReg.b16, regAVReg.b16);
		fsub(TEMP_FPR1.VReg.d2, regRVReg.d2, TEMP_FPR1.VReg.d2);
		mov(regRVReg.d[0], TEMP_FPR1.VReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_ASSIGN)
	{
		mov(regRVReg.b16, regAVReg.b16);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_BOTTOM_FCTIWZ)
	{
		fcvtzs(TEMP_GPR1.WReg, regADReg);
		mov(regRVReg.d[0], TEMP_GPR1.XReg);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_NEGATE_PAIR)
	{
		fneg(regRVReg.d2, regAVReg.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ABS_PAIR)
	{
		fabs(regRVReg.d2, regAVReg.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_FRES_PAIR || imlInstruction->operation == PPCREC_IML_OP_FPR_FRSQRTE_PAIR)
	{
		uintptr_t routine = imlInstruction->operation == PPCREC_IML_OP_FPR_FRES_PAIR ? (uintptr_t)recompiler_fres : (uintptr_t)recompiler_frsqrte;
		mov(TEMP_GPR2.XReg, x30);
		mov(TEMP_GPR1.XReg, routine);
		mov(ASM_ROUTINE_FPR.VReg.d[0], regAVReg.d[0]);
		blr(TEMP_GPR1.XReg);
		mov(regRVReg.d[0], ASM_ROUTINE_FPR.VReg.d[0]);
		mov(ASM_ROUTINE_FPR.VReg.d[0], regAVReg.d[1]);
		blr(TEMP_GPR1.XReg);
		mov(regRVReg.d[1], ASM_ROUTINE_FPR.VReg.d[0]);
		mov(x30, TEMP_GPR2.XReg);
	}
	else
	{
		cemu_assert_suspicious();
	}
}

void AArch64GenContext_t::fpr_r_r_r(IMLInstruction* imlInstruction)
{
	auto regR = fpReg<VReg>(imlInstruction->op_fpr_r_r_r.regR);
	auto regA = fpReg<VReg>(imlInstruction->op_fpr_r_r_r.regA);
	auto regB = fpReg<VReg>(imlInstruction->op_fpr_r_r_r.regB);

	if (imlInstruction->operation == PPCREC_IML_OP_FPR_MULTIPLY_BOTTOM)
	{
		fmul(TEMP_FPR1.VReg.d2, regA.d2, regB.d2);
		mov(regR.d[0], TEMP_FPR1.VReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ADD_BOTTOM)
	{
		fadd(TEMP_FPR1.DReg, fpReg<DReg>(imlInstruction->op_fpr_r_r_r.regA), fpReg<DReg>(imlInstruction->op_fpr_r_r_r.regB));
		if (regR.getIdx() == regA.getIdx())
		{
			mov(regR.d[0], TEMP_FPR1.VReg.d[0]);
		}
		else
		{
			mov(TEMP_FPR1.VReg.d[1], regA.d[1]);
			mov(regR.b16, TEMP_FPR1.VReg.b16);
		}
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SUB_PAIR)
	{
		fsub(regR.d2, regA.d2, regB.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SUB_BOTTOM)
	{
		fsub(TEMP_FPR1.VReg.d2, regA.d2, regB.d2);
		mov(regR.d[0], TEMP_FPR1.VReg.d[0]);
	}
	else
	{
		cemu_assert_suspicious();
	}
}

/*
 * FPR = op (fprA, fprB, fprC)
 */
void AArch64GenContext_t::fpr_r_r_r_r(IMLInstruction* imlInstruction)
{
	auto regR = fpReg<VReg>(imlInstruction->op_fpr_r_r_r_r.regR);
	auto regA = fpReg<VReg>(imlInstruction->op_fpr_r_r_r_r.regA);
	auto regB = fpReg<VReg>(imlInstruction->op_fpr_r_r_r_r.regB);
	auto regC = fpReg<VReg>(imlInstruction->op_fpr_r_r_r_r.regC);

	if (imlInstruction->operation == PPCREC_IML_OP_FPR_SUM0)
	{
		dup(TEMP_FPR1.VReg.d2, regB.d[1]);
		fadd(regR.d2, regA.d2, TEMP_FPR1.VReg.d2);
		mov(regR.d[1], regC.d[1]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SUM1)
	{
		dup(TEMP_FPR1.VReg.d2, regB.d[1]);
		fadd(TEMP_FPR1.VReg.d2, TEMP_FPR1.VReg.d2, regA.d2);
		zip1(regR.d2, regC.d2, TEMP_FPR1.VReg.d2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SELECT_BOTTOM)
	{
		auto regADReg = fpReg<DReg>(imlInstruction->op_fpr_r_r_r_r.regA);
		auto regBDReg = fpReg<DReg>(imlInstruction->op_fpr_r_r_r_r.regB);
		auto regCDReg = fpReg<DReg>(imlInstruction->op_fpr_r_r_r_r.regC);
		fcmp(regADReg, 0.0);
		fcsel(TEMP_FPR1.DReg, regCDReg, regBDReg, Cond::GE);
		mov(regR.d[0], TEMP_FPR1.VReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_SELECT_PAIR)
	{
		if (regR.getIdx() != regB.getIdx() && regR.getIdx() != regC.getIdx())
		{
			fcmge(regR.d2, regA.d2, 0.0);
			bsl(regR.b16, regC.b16, regB.b16);
		}
		else
		{
			fcmge(TEMP_FPR1.VReg.d2, regA.d2, 0.0);
			bsl(TEMP_FPR1.VReg.b16, regC.b16, regB.b16);
			mov(regR.b16, TEMP_FPR1.VReg.b16);
		}
	}
	else
	{
		cemu_assert_suspicious();
	}
}

void AArch64GenContext_t::fpr_r(IMLInstruction* imlInstruction)
{
	auto regRVReg = fpReg<VReg>(imlInstruction->op_fpr_r.regR);
	auto regRDReg = fpReg<DReg>(imlInstruction->op_fpr_r.regR);
	auto regRSReg = fpReg<SReg>(imlInstruction->op_fpr_r.regR);

	if (imlInstruction->operation == PPCREC_IML_OP_FPR_NEGATE_BOTTOM)
	{
		fneg(TEMP_FPR1.DReg, regRDReg);
		mov(regRVReg.d[0], TEMP_FPR1.VReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ABS_BOTTOM)
	{
		fabs(TEMP_FPR1.DReg, regRDReg);
		mov(regRVReg.d[0], TEMP_FPR1.VReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_NEGATIVE_ABS_BOTTOM)
	{
		fabs(TEMP_FPR1.DReg, regRDReg);
		fneg(TEMP_FPR1.DReg, TEMP_FPR1.DReg);
		mov(regRVReg.d[0], TEMP_FPR1.VReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ROUND_TO_SINGLE_PRECISION_BOTTOM)
	{
		// convert to 32bit single
		fcvt(TEMP_FPR1.SReg, regRDReg);
		// convert back to 64bit double
		fcvt(TEMP_FPR1.DReg, TEMP_FPR1.SReg);
		mov(regRVReg.d[0], TEMP_FPR1.VReg.d[0]);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_ROUND_TO_SINGLE_PRECISION_PAIR)
	{
		// convert to 32bit singles
		fcvtn(regRVReg.s2, regRVReg.d2);
		// convert back to 64bit doubles
		fcvtl(regRVReg.d2, regRVReg.s2);
	}
	else if (imlInstruction->operation == PPCREC_IML_OP_FPR_EXPAND_BOTTOM32_TO_BOTTOM64_AND_TOP64)
	{
		// convert bottom to 64bit double
		fcvt(regRDReg, regRSReg);
		// copy to top half
		dup(regRVReg.d2, regRVReg.d[0]);
	}
	else
	{
		cemu_assert_unimplemented();
	}
}

Cond ImlFPCondToArm64Cond(IMLCondition cond)
{
	switch (cond)
	{
	case IMLCondition::UNORDERED_GT:
		return Cond::GT;
	case IMLCondition::UNORDERED_LT:
		return Cond::MI;
	case IMLCondition::UNORDERED_EQ:
		return Cond::EQ;
	case IMLCondition::UNORDERED_U:
		return Cond::VS;
	default:
	{
		cemu_assert_suspicious();
		return Cond::EQ;
	}
	}
}

void AArch64GenContext_t::fpr_compare(IMLInstruction* imlInstruction)
{
	auto regR = gpReg<XReg>(imlInstruction->op_fpr_compare.regR);
	auto regA = fpReg<DReg>(imlInstruction->op_fpr_compare.regA);
	auto regB = fpReg<DReg>(imlInstruction->op_fpr_compare.regB);
	auto cond = ImlFPCondToArm64Cond(imlInstruction->op_fpr_compare.cond);
	fcmp(regA, regB);
	cset(regR, cond);
}

void AArch64GenContext_t::call_imm(IMLInstruction* imlInstruction)
{
	str(x30, AdrPreImm(sp, -16));
	mov(TEMP_GPR1.XReg, imlInstruction->op_call_imm.callAddress);
	blr(TEMP_GPR1.XReg);
	ldr(x30, AdrPostImm(sp, 16));
}

bool PPCRecompiler_generateAArch64Code(struct PPCRecFunction_t* PPCRecFunction, struct ppcImlGenContext_t* ppcImlGenContext)
{
	AArch64Allocator allocator;
	AArch64GenContext_t aarch64GenContext{&allocator};

	// generate iml instruction code
	bool codeGenerationFailed = false;
	for (IMLSegment* segIt : ppcImlGenContext->segmentList2)
	{
		if (codeGenerationFailed)
			break;
		segIt->x64Offset = aarch64GenContext.getSize();

		aarch64GenContext.storeSegmentStart(segIt);

		for (size_t i = 0; i < segIt->imlList.size(); i++)
		{
			IMLInstruction* imlInstruction = segIt->imlList.data() + i;
			if (imlInstruction->type == PPCREC_IML_TYPE_R_NAME)
			{
				aarch64GenContext.r_name(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_NAME_R)
			{
				aarch64GenContext.name_r(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_R_R)
			{
				if (!aarch64GenContext.r_r(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_R_S32)
			{
				if (!aarch64GenContext.r_s32(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_R_R_S32)
			{
				if (!aarch64GenContext.r_r_s32(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_R_R_S32_CARRY)
			{
				if (!aarch64GenContext.r_r_s32_carry(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_R_R_R)
			{
				if (!aarch64GenContext.r_r_r(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_R_R_R_CARRY)
			{
				if (!aarch64GenContext.r_r_r_carry(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_COMPARE)
			{
				aarch64GenContext.compare(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_COMPARE_S32)
			{
				aarch64GenContext.compare_s32(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_CONDITIONAL_JUMP)
			{
				if (segIt->nextSegmentBranchTaken == segIt)
					cemu_assert_suspicious();
				aarch64GenContext.cjump(imlInstruction, segIt);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_JUMP)
			{
				aarch64GenContext.jump(segIt);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_CJUMP_CYCLE_CHECK)
			{
				aarch64GenContext.conditionalJumpCycleCheck(segIt);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_MACRO)
			{
				if (!aarch64GenContext.macro(imlInstruction))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_LOAD)
			{
				if (!aarch64GenContext.load(imlInstruction, false))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_LOAD_INDEXED)
			{
				if (!aarch64GenContext.load(imlInstruction, true))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_STORE)
			{
				if (!aarch64GenContext.store(imlInstruction, false))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_STORE_INDEXED)
			{
				if (!aarch64GenContext.store(imlInstruction, true))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_ATOMIC_CMP_STORE)
			{
				aarch64GenContext.atomic_cmp_store(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_NO_OP)
			{
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_CALL_IMM)
			{
				aarch64GenContext.call_imm(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_LOAD)
			{
				if (!aarch64GenContext.fpr_load(imlInstruction, false))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_LOAD_INDEXED)
			{
				if (!aarch64GenContext.fpr_load(imlInstruction, true))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_STORE)
			{
				if (!aarch64GenContext.fpr_store(imlInstruction, false))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_STORE_INDEXED)
			{
				if (!aarch64GenContext.fpr_store(imlInstruction, true))
					codeGenerationFailed = true;
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_R_R)
			{
				aarch64GenContext.fpr_r_r(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_R_R_R)
			{
				aarch64GenContext.fpr_r_r_r(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_R_R_R_R)
			{
				aarch64GenContext.fpr_r_r_r_r(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_R)
			{
				aarch64GenContext.fpr_r(imlInstruction);
			}
			else if (imlInstruction->type == PPCREC_IML_TYPE_FPR_COMPARE)
			{
				aarch64GenContext.fpr_compare(imlInstruction);
			}
			else
			{
				codeGenerationFailed = true;
				cemu_assert_suspicious();
				cemuLog_log(LogType::Recompiler, "PPCRecompiler_generateX64Code(): Unsupported iml type {:x}", imlInstruction->type);
			}
		}
	}

	// handle failed code generation
	if (codeGenerationFailed)
	{
		return false;
	}

	if (!aarch64GenContext.processAllJumps())
	{
		return false;
	}

	aarch64GenContext.readyRE();

	// set code
	PPCRecFunction->x86Code = aarch64GenContext.getCode<void*>();
	PPCRecFunction->x86Size = aarch64GenContext.getMaxSize();
	// set free disabled to skip freeing the code from the CodeGenerator destructor
	allocator.setFreeDisabled(true);
	return true;
}

void PPCRecompiler_cleanupAArch64Code(void* code, size_t size)
{
	AArch64Allocator allocator;
	if (allocator.useProtect())
		CodeArray::protect(code, size, CodeArray::PROTECT_RW);
	allocator.free(static_cast<uint32_t*>(code));
}

void AArch64GenContext_t::enterRecompilerCode()
{
	constexpr size_t STACK_SIZE = 160 /* x19 .. x30 + v8.d[0] .. v15.d[0] */;
	static_assert(STACK_SIZE % 16 == 0);
	sub(sp, sp, STACK_SIZE);
	mov(x9, sp);

	stp(x19, x20, AdrPostImm(x9, 16));
	stp(x21, x22, AdrPostImm(x9, 16));
	stp(x23, x24, AdrPostImm(x9, 16));
	stp(x25, x26, AdrPostImm(x9, 16));
	stp(x27, x28, AdrPostImm(x9, 16));
	stp(x29, x30, AdrPostImm(x9, 16));
	st4((v8.d - v11.d)[0], AdrPostImm(x9, 32));
	st4((v12.d - v15.d)[0], AdrPostImm(x9, 32));
	mov(HCPU_REG, x1); // call argument 2
	mov(PPC_REC_INSTANCE_REG, (uint64)ppcRecompilerInstanceData);
	mov(MEM_BASE_REG, (uint64)memory_base);

	// branch to recFunc
	blr(x0); // call argument 1

	mov(x9, sp);
	ldp(x19, x20, AdrPostImm(x9, 16));
	ldp(x21, x22, AdrPostImm(x9, 16));
	ldp(x23, x24, AdrPostImm(x9, 16));
	ldp(x25, x26, AdrPostImm(x9, 16));
	ldp(x27, x28, AdrPostImm(x9, 16));
	ldp(x29, x30, AdrPostImm(x9, 16));
	ld4((v8.d - v11.d)[0], AdrPostImm(x9, 32));
	ld4((v12.d - v15.d)[0], AdrPostImm(x9, 32));

	add(sp, sp, STACK_SIZE);

	ret();
}

void AArch64GenContext_t::leaveRecompilerCode()
{
	str(LR.WReg, AdrImm(HCPU_REG, offsetof(PPCInterpreter_t, instructionPointer)));
	ret();
}

bool initializedInterfaceFunctions = false;
AArch64GenContext_t enterRecompilerCode_ctx{};

AArch64GenContext_t leaveRecompilerCode_unvisited_ctx{};
AArch64GenContext_t leaveRecompilerCode_visited_ctx{};
void PPCRecompilerAArch64Gen_generateRecompilerInterfaceFunctions()
{
	if (initializedInterfaceFunctions)
		return;
	initializedInterfaceFunctions = true;

	enterRecompilerCode_ctx.enterRecompilerCode();
	enterRecompilerCode_ctx.readyRE();
	PPCRecompiler_enterRecompilerCode = enterRecompilerCode_ctx.getCode<decltype(PPCRecompiler_enterRecompilerCode)>();

	leaveRecompilerCode_unvisited_ctx.leaveRecompilerCode();
	leaveRecompilerCode_unvisited_ctx.readyRE();
	PPCRecompiler_leaveRecompilerCode_unvisited = leaveRecompilerCode_unvisited_ctx.getCode<decltype(PPCRecompiler_leaveRecompilerCode_unvisited)>();

	leaveRecompilerCode_visited_ctx.leaveRecompilerCode();
	leaveRecompilerCode_visited_ctx.readyRE();
	PPCRecompiler_leaveRecompilerCode_visited = leaveRecompilerCode_visited_ctx.getCode<decltype(PPCRecompiler_leaveRecompilerCode_visited)>();
}
