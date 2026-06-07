.syntax unified
.cpu cortex-m3
.thumb

    /* Interrupt Vector Table Entry for EXTI0 (Button on Pin 0) */
    .global EXTI0_IRQHandler
    .type EXTI0_IRQHandler, %function

EXTI0_IRQHandler:
    /* 1. Save Context */
    PUSH {R0-R4, LR}

    /* 2. Read the Pending Register to see which line triggered */
    LDR R0, =0x40010008       /* EXTI_PR (Pending Register) */
    LDR R1, [R0]
    
    /* Check if Bit 0 is set (Button 0) */
    TST R1, #1
    BEQ exit_isr              /* If not our button, exit */

    /* 3. ACTUATE HARDWARE (e.g., Toggle an LED) */
    LDR R0, =0x40020014       /* GPIOA_ODR */
    LDR R2, [R0]
    EOR R2, R2, #0x04         /* Toggle Bit 2 (Green LED) */
    STR R2, [R0]

    /* 4. Clear the Interrupt Pending Bit (Crucial!) */
    LDR R0, =0x40010008       /* EXTI_PR */
    MOV R1, #1
    STR R1, [R0]              /* Writing 1 clears the flag */

exit_isr:
    /* 5. Restore Context and Return from Interrupt */
    POP {R0-R4, PC}           /* PC = Return Address */

    .end
