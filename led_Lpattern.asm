.syntax unified
.cpu cortex-m3
.thumb

    .equ RCC_APB2ENR,     0x40021018  /* Clock Enable Register (Example) */
    .equ GPIOA_MODER,     0x40020000  /* Mode Register (Input/Output) */
    .equ GPIOA_ODR,       0x40020014  /* Output Data Register (The lights) */
    
    /* --- Light States (Bit masks) --- */
    /* Assuming: Bit 0 = Red, Bit 1 = Yellow, Bit 2 = Green */
    .equ LIGHT_RED,       0x01
    .equ LIGHT_YELLOW,    0x02
    .equ LIGHT_GREEN,     0x04

    /* Adjust these based on your actual CPU frequency */
    .equ DELAY_RED_MS, 5000        /* 5 seconds */
    .equ DELAY_GREEN_MS, 5000        /* 5 seconds */
    .equ DELAY_YELLOW_MS, 2000        /* 2 seconds */

    .global _start

_start:
    /* 1. SYSTEM INITIALIZATION */
    /* Enable Clock for GPIO Port A */
    LDR R0, =RCC_APB2ENR
    LDR R1, [R0]
    ORR R1, R1, #0x04           /* Enable GPIOA clock (bit 2 usually) */
    STR R1, [R0]

    /* Configure GPIO Pins as Output (Simplified setup) */
    /* Assuming bits 0, 1, 2 are our lights. Set them to Output mode (01) */
    LDR R0, =GPIOA_MODER
    LDR R1, [R0]
    /* Clear bits 0,1,2,3,4,5 first (simplest way for just 3 pins) */
    BIC R1, R1, #0x3F           
    /* Set bits 0,1,2 to Output (01) -> Value 0x15 (binary 010101) */
    ORR R1, R1, #0x15           
    STR R1, [R0]

    /* Initialize Output Register to 0 (All lights off initially) */
    LDR R0, =GPIOA_ODR
    MOV R1, #0
    STR R1, [R0]

    /* 2. MAIN LOOP */
main_loop:
    
    
    LDR R0, =GPIOA_ODR
    MOV R1, #LIGHT_GREEN
    STR R1, [R0]
    
    /* Call Delay for Green (5 seconds) */
    BL delay_ms
    MOV R0, #DELAY_GREEN_MS
    BL delay_ms_wrapper

    
    MOV R1, #LIGHT_YELLOW
    STR R1, [R0]
    
    /* Call Delay for Yellow (2 seconds) */
    MOV R0, #DELAY_YELLOW_MS
    BL delay_ms_wrapper

    
    MOV R1, #LIGHT_RED
    STR R1, [R0]
    
    /* Call Delay for Red (5 seconds) */
    MOV R0, #DELAY_RED_MS
    BL delay_ms_wrapper

    /* Loop forever */
    B main_loop

/* --- DELAY SUBROUTINE --- */
/* Input: R0 = Milliseconds to delay */
/* This is a simplified software loop. Real systems use Hardware Timers. */
delay_ms_wrapper:
    PUSH {R1-R3, LR}        /* Save registers */
    
    /* Simple loop counter approximation 
       For 10MHz clock: ~10000 cycles per ms (very rough) 
       We need a nested loop for longer delays */
    
    MOV R1, R0              /* R1 = outer loop count (ms) */
    
outer_loop:
    PUSH {R1}
    MOV R2, #10000          /* Inner loop count (approx 1ms) */
    
inner_loop:
    SUBS R2, R2, #1
    BNE inner_loop
    
    POP {R1}
    SUBS R1, R1, #1
    BNE outer_loop

    POP {R1-R3, PC}         /* Return */

/* --- HELPER: Generic Delay (Original style) --- */
/* If you just want a raw delay without arguments */
DELAY:
    MOV R2, #50000
delay_loop:
    SUBS R2, R2, #1
    BNE delay_loop
    BX LR

/* --- DATA SECTION --- */
    .end
