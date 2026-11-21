/* SPDX-License-Identifier: GPL-3.0-only */
/* Capture one band of LCD writes at a time. No framebuffer allocation, DMA
 * waits or USB operations in the LCD hooks. This unit is included by main.c. */
#define SM_BUFFER 1200u
#include "screen_mirror.c"

#define MIRROR_WIDTH 240u
#define MIRROR_BAND 8u
static uint16_t mirror_pixels[MIRROR_BAND][MIRROR_WIDTH]
    __attribute__((section(".pool.mirror")));
static unsigned mirror_top, mirror_row;
static uint16_t mirror_background;
static uint8_t mirror_capturing, mirror_ready, mirror_identify;
static volatile uint8_t mirror_request;
static volatile uint8_t mirror_full_request;

static void mirror_clear(unsigned x, unsigned y, unsigned w, unsigned h, uint16_t color)
{
    if (x == 0 && y == 0 && w == MIRROR_WIDTH && h == MIRROR_WIDTH)
        mirror_background = color;
    if (!mirror_capturing) return;
    for (unsigned row = mirror_top; row < mirror_top + MIRROR_BAND; ++row)
        if (row >= y && row < y + h)
            for (unsigned col = x; col < x + w; ++col)
                mirror_pixels[row - mirror_top][col] = color;
}

static void mirror_blit(unsigned x, unsigned y, unsigned w, unsigned h, const uint16_t *pixels)
{
    if (!mirror_capturing || x >= MIRROR_WIDTH) return;
    const unsigned width = w > MIRROR_WIDTH - x ? MIRROR_WIDTH - x : w;
    for (unsigned row = mirror_top; row < mirror_top + MIRROR_BAND; ++row) {
        if (row < y || row >= y + h) continue;
        const uint16_t *source = pixels + (row - y) * w;
        uint16_t *target = mirror_pixels[row - mirror_top] + x;
        for (unsigned col = 0; col < width; ++col) {
            const uint16_t color = source[col];          /* LCD DMA uses big-endian RGB565 */
            target[col] = (uint16_t)(color >> 8 | color << 8);
        }
    }
}

static int mirror_begin(void)
{
    if (!sm_on() || mirror_ready) return 0;
    mirror_capturing = 1;
    mirror_clear(0, mirror_top, MIRROR_WIDTH, MIRROR_BAND, mirror_background);
    return 1;
}

static void mirror_finish(void)
{
    if (!mirror_capturing) return;
    mirror_capturing = 0;
    mirror_ready = 1;
    mirror_row = 0;
}
