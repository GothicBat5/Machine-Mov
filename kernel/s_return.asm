#ifndef CONFIG_CPU_THUMBONLY
#define ARM_OK(code...)	code
#else
#define ARM_OK(code...)
#endif

	.macro arm_slot n
	.org	sigreturn_codes + 12 * (\n)
ARM_OK(	.arm	)
	.endm

	.macro thumb_slot n
	.org	sigreturn_codes + 12 * (\n) + 8
	.thumb
	.endm

	.macro arm_fdpic_slot n
	.org	sigreturn_codes + 24 + 20 * (\n)
ARM_OK(	.arm	)
	.endm

	.macro thumb_fdpic_slot n
	.org	sigreturn_codes + 24 + 20 * (\n) + 12
	.thumb
	.endm

#if __LINUX_ARM_ARCH__ <= 4

	.arch armv4t
#endif

	.section .rodata
	.global sigreturn_codes
	.type	sigreturn_codes, #object

	.align

sigreturn_codes:

	/* ARM sigreturn syscall code snippet */
	arm_slot 0
ARM_OK(	mov	r7, #(__NR_sigreturn - __NR_SYSCALL_BASE)	)
ARM_OK(	swi	#(__NR_sigreturn)|(__NR_OABI_SYSCALL_BASE)	)

	/* Thumb sigreturn syscall code snippet */
	thumb_slot 0
	movs r7, #(__NR_sigreturn - __NR_SYSCALL_BASE)
	swi	#0

	/* ARM sigreturn_rt syscall code snippet */
	arm_slot 1
ARM_OK(	mov	r7, #(__NR_rt_sigreturn - __NR_SYSCALL_BASE)	)
ARM_OK(	swi	#(__NR_rt_sigreturn)|(__NR_OABI_SYSCALL_BASE)	)

	/* Thumb sigreturn_rt syscall code snippet */
	thumb_slot 1
	movs	r7, #(__NR_rt_sigreturn - __NR_SYSCALL_BASE)
	swi	#0

	/* ARM sigreturn restorer FDPIC bounce code snippet */
	arm_fdpic_slot 0
ARM_OK(	ldr	r3, [sp, #SIGFRAME_RC3_OFFSET] )
ARM_OK(	ldmia	r3, {r3, r9} )
#ifdef CONFIG_ARM_THUMB
ARM_OK(	bx	r3 )
#else
ARM_OK(	ret	r3 )
#endif

	/* Thumb sigreturn restorer FDPIC bounce code snippet */
	thumb_fdpic_slot 0
	ldr	r3, [sp, #SIGFRAME_RC3_OFFSET]
	ldmia	r3, {r2, r3}
	mov	r9, r3
	bx	r2

	/* ARM sigreturn_rt restorer FDPIC bounce code snippet */
	arm_fdpic_slot 1
ARM_OK(	ldr	r3, [sp, #RT_SIGFRAME_RC3_OFFSET] )
ARM_OK(	ldmia	r3, {r3, r9} )
#ifdef CONFIG_ARM_THUMB
ARM_OK(	bx	r3 )
#else
ARM_OK(	ret	r3 )
#endif

	/* Thumb sigreturn_rt restorer FDPIC bounce code snippet */
	thumb_fdpic_slot 1
	ldr	r3, [sp, #RT_SIGFRAME_RC3_OFFSET]
	ldmia	r3, {r2, r3}
	mov	r9, r3
	bx	r2

	.space	4

	.size	sigreturn_codes, . - sigreturn_codes
