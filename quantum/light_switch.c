#include <stdio.h>
#include <stdbool.h>
#include <unistd.h>

typedef struct 
{
    bool is_on;
} light_switch;

void init_switch(light_switch *sw)
{
    sw->is_on = false;
}

void turn_on(light_switch *sw)
{
    sw->is_on = true;
}

void turn_off(light_switch *sw)
{
    sw->is_on = false;
    printf("\nThe light is off\n");
}

void toggle(light_switch *sw)
{
    sw->is_on = !sw->is_on;
    printf("\nLight has been toggled\n");
}

void status(const light_switch *sw)\
{
    printf("\nStatus: %s\n", sw->is_on ? "ON" : "OFF");
}

void turnOnForSeconds(light_switch *sw, int seconds) 
{
    sw->is_on = true;
    printf("The light is ON for %d seconds...\n", seconds);

    //Sleep(seconds * 1000);
    sleep(seconds);

    sw->is_on = false;
    printf("The light is OFF.\n");
}

int main()
{
    light_switch mySwitch;
    init_switch(&mySwitch);

    int sec;
    printf("Seconds: ");
    scanf("%d", &sec);

    turnOnForSeconds(&mySwitch, sec);
    status(&mySwitch);

    return 0;
}
