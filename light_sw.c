/*
 * Control LED - Traffic Light Sequence
 */
#include <iso646.h>
#include <stdint.h>
#include <stdckdint.h>
#include <stdatomic.h>
#include <stdalign.h>
#include <stdnoreturn.h>

/* Hardware Definitions */
#define GPIO_BASE_ADDR 0x4000
#define GPIO_PORT (*(volatile uint32_t *)GPIO_BASE_ADDR)

/* Pin Masks */
#define PIN_RED_LED      (1U << 0)
#define PIN_YELLOW_LED   (1U << 1)
#define PIN_GREEN_LED    (1U << 2)


/* Timing */
#define DELAY_CYCLES        500000U

/* Safe delay function with volatile to prevent compiler optimization */
void delay(uint32_t cycles)
{
    volatile uint32_t i;
    for (i = 0; i < cycles; i++)
    {
        /* Empty loop to waste time */
    }
}

/* Helper to set a specific LED without affecting others */
void set_led(uint32_t led_mask)
{
    GPIO_PORT = led_mask; 
    /* 
     * NOTE: If this hardware requires 'Set/Clear' registers, 
     * you would use:
     * GPIO_PORT_SET = led_mask;
     * GPIO_PORT_CLEAR = ~led_mask; 
     * But for this simple example, direct assignment is shown.
     */
}

int main(void)
{
    /* 
     * TODO: Initialize GPIO Direction here.
     * Example: GPIO_DIR_REG |= (PIN_RED_LED | PIN_YELLOW_LED | PIN_GREEN_LED);
     */

    while (1)
    {
        
        GPIO_PORT = PIN_RED_LED;       /* Clears others, sets Red */
        delay(DELAY_CYCLES);

        
        GPIO_PORT = PIN_GREEN_LED;     /* Clears others, sets Green */
        delay(DELAY_CYCLES);

       
        GPIO_PORT = PIN_YELLOW_LED;    /* Clears others, sets Yellow */
        delay(DELAY_CYCLES);
    }

    return 0;
}
