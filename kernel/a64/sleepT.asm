
.macro compute_mpidr_hash dst, rs0, rs1, rs2, rs3, mpidr, mask
	and	\mpidr, \mpidr, \mask		// mask out MPIDR bits
	and	\dst, \mpidr, #0xff		// mask=aff0
	lsr	\dst ,\dst, \rs0		// dst=aff0>>rs0
	and	\mask, \mpidr, #0xff00		// mask = aff1
	lsr	\mask ,\mask, \rs1
	orr	\dst, \dst, \mask		// dst|=(aff1>>rs1)
	and	\mask, \mpidr, #0xff0000	// mask = aff2
	lsr	\mask ,\mask, \rs2
	orr	\dst, \dst, \mask		// dst|=(aff2>>rs2)
	and	\mask, \mpidr, #0xff00000000	// mask = aff3
	lsr	\mask ,\mask, \rs3
	orr	\dst, \dst, \mask		// dst|=(aff3>>rs3)
	.endm
SYM_FUNC_START(__cpu_suspend_enter)
	stp	x29, lr, [x0, #SLEEP_STACK_DATA_CALLEE_REGS]
	stp	x19, x20, [x0,#SLEEP_STACK_DATA_CALLEE_REGS+16]
	stp	x21, x22, [x0,#SLEEP_STACK_DATA_CALLEE_REGS+32]
	stp	x23, x24, [x0,#SLEEP_STACK_DATA_CALLEE_REGS+48]
	stp	x25, x26, [x0,#SLEEP_STACK_DATA_CALLEE_REGS+64]
	stp	x27, x28, [x0,#SLEEP_STACK_DATA_CALLEE_REGS+80]
	mov	x2, sp
	str	x2, [x0, #SLEEP_STACK_DATA_SYSTEM_REGS + CPU_CTX_SP]
	ldr_l	x1, sleep_save_stash
	mrs	x7, mpidr_el1
	adr_l	x9, mpidr_hash
	ldr	x10, [x9, #MPIDR_HASH_MASK]
	ldp	w3, w4, [x9, #MPIDR_HASH_SHIFTS]
	ldp	w5, w6, [x9, #(MPIDR_HASH_SHIFTS + 8)]
	compute_mpidr_hash x8, x3, x4, x5, x6, x7, x10
	add	x1, x1, x8, lsl #3
	str	x0, [x1]
	add	x0, x0, #SLEEP_STACK_DATA_SYSTEM_REGS
	stp	x29, lr, [sp, #-16]!
	bl	cpu_do_suspend
	ldp	x29, lr, [sp], #16
	mov	x0, #1
	ret
SYM_FUNC_END(__cpu_suspend_enter)

	.pushsection ".idmap.text", "a"
SYM_CODE_START(cpu_resume)
	mov	x0, xzr
	bl	init_kernel_el
	mov	x19, x0			// preserve boot mode
	bl	__cpu_setup
	adrp	x1, swapper_pg_dir
	adrp	x2, idmap_pg_dir
	bl	__enable_mmu
	ldr	x8, =_cpu_resume
	br	x8
SYM_CODE_END(cpu_resume)
	.ltorg
	.popsection
SYM_FUNC_START(_cpu_resume)
	mov	x0, x19
	bl	finalise_el2
	mrs	x1, mpidr_el1
	adr_l	x8, mpidr_hash// x8 = struct mpidr_hash virt address
	ldr	x2, [x8, #MPIDR_HASH_MASK]
	ldp	w3, w4, [x8, #MPIDR_HASH_SHIFTS]
	ldp	w5, w6, [x8, #(MPIDR_HASH_SHIFTS + 8)]
	compute_mpidr_hash x7, x3, x4, x5, x6, x1, x2
	ldr_l	x0, sleep_save_stash
	ldr	x0, [x0, x7, lsl #3]
	add	x29, x0, #SLEEP_STACK_DATA_CALLEE_REGS
	add	x0, x0, #SLEEP_STACK_DATA_SYSTEM_REGS
	ldr	x2, [x0, #CPU_CTX_SP]
	mov	sp, x2
	bl	cpu_do_resume
	mov	x0, sp
	bl	kasan_unpoison_task_stack_below
	ldp	x19, x20, [x29, #16]
	ldp	x21, x22, [x29, #32]
	ldp	x23, x24, [x29, #48]
	ldp	x25, x26, [x29, #64]
	ldp	x27, x28, [x29, #80]
	ldp	x29, lr, [x29]
	mov	x0, #0
	ret
SYM_FUNC_END(_cpu_resume)
