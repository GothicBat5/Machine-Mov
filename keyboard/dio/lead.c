#include <string.h>
#include "leds.h"
#include "i2c_master.h"
#include "led_tables.h"
#include "rgb_matrix.h"
#include "model01.h"

#define I2C_TIMEOUT 1000

void set_all_leds_to(uint8_t r, uint8_t g, uint8_t b) 
{
  uint8_t buf[] = {
    TWI_CMD_LED_SET_ALL_TO,
    b, g, r
  };
  i2c_transmit(I2C_ADDR(LEFT), buf, sizeof(buf), I2C_TIMEOUT);
  i2c_transmit(I2C_ADDR(RIGHT), buf, sizeof(buf), I2C_TIMEOUT);
  _delay_us(10);
}

void set_led_to(int led, uint8_t r, uint8_t g, uint8_t b) 
{
  uint8_t buf[] = {
    TWI_CMD_LED_SET_ONE_TO,
    led & 0x1f,
    b, g, r
  };
  int hand = (led >= 32) ? RIGHT : LEFT;
  i2c_transmit(I2C_ADDR(hand), buf, sizeof(buf), I2C_TIMEOUT);
  _delay_us(10);
}

#ifdef RGB_MATRIX_ENABLE

static struct {
  uint8_t b;
  uint8_t g;
  uint8_t r;
} __attribute__((packed)) led_state[64];

static void set_color(int index, uint8_t r, uint8_t g, uint8_t b) {
  led_state[index].r = r;
  led_state[index].g = g;
  led_state[index].b = b;
}

static void set_color_all(uint8_t r, uint8_t g, uint8_t b) {
  for (int i=0; i<RGB_MATRIX_LED_COUNT; i++)
    set_color(i, r, g, b);
}

static void init(void) {
  gpio_set_pin_output(E6);
  gpio_write_pin_low(E6);
  gpio_set_pin_input(B4);
}

static void flush(void) {
  uint8_t *bank_data = (uint8_t*)&led_state[0];
  uint8_t command[1 + 8*3];
  
  for (int hand=0; hand<2; hand++) 
  {
    int addr = I2C_ADDR(hand);
    for (int bank=0; bank<4; bank++) 
    {
      command[0] = TWI_CMD_LED_BASE + bank;
      memcpy(&command[1], bank_data, 8*3);
      i2c_transmit(addr, command, sizeof(command), I2C_TIMEOUT);
      _delay_us(100);

      bank_data += 8*3;
    }
  }
}

const rgb_matrix_driver_t rgb_matrix_driver = {
  .init = init,
  .flush = flush,
  .set_color = set_color,
  .set_color_all = set_color_all
};

#endif

/* vim: set ts=2 sw=2 et: */
