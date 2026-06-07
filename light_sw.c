/*
 * Control LED - timer
 * Brief >> example
 */

#include <stdint.h>

#define GPIO_PORT (*(volatile uint32_t*)0x4000)

#define RED_LED (1 << 0)
#define YELLOW_LED (1 << 1)
#define GREEN_LED (1 << 2)

void delay(void)
{
    volatile uint32_t i;

    for (i = 0; i < 500000; i++)
    {
        /* waste time */
    }
}

int main(void)
{
    while (1)
    {
        GPIO_PORT = RED_LED;
        delay();

        GPIO_PORT = GREEN_LED;
        delay();

        GPIO_PORT = YELLOW_LED;
        delay();
    }

    return 0;
}
