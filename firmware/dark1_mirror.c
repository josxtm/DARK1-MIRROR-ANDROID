/* SPDX-License-Identifier: GPL-3.0-only */
/* A small rolling copy of LCD writes. Send changed 60 x 4 tiles as ordinary
 * Big Screen row segments, and revisit the knob values first after input. */
#define SM_BUFFER 1200u
#include "screen_mirror.c"

#define MIRROR_WIDTH 240u
#define MIRROR_BAND 12u
#define MIRROR_POOL_ROWS 8u
#define MIRROR_TILE_W 60u
#define MIRROR_TILE_H 4u
#define MIRROR_COLS (MIRROR_WIDTH / MIRROR_TILE_W)
#define MIRROR_TILES (MIRROR_WIDTH / MIRROR_TILE_H * MIRROR_COLS)

static uint16_t mirror_pool[MIRROR_POOL_ROWS][MIRROR_WIDTH] __attribute__((section(".pool.mirror")));
static uint16_t mirror_extra[MIRROR_BAND - MIRROR_POOL_ROWS][MIRROR_WIDTH];
static uint32_t mirror_hashes[MIRROR_TILES];
static uint32_t mirror_pending[MIRROR_COLS];
static unsigned mirror_scan, mirror_focus_top, mirror_focus_left;
static uint32_t mirror_focus_time;
static unsigned mirror_top, mirror_height, mirror_offset, mirror_row, mirror_covered;
static unsigned mirror_dirty, mirror_first, mirror_span;
static uint16_t mirror_background;
static uint8_t mirror_capturing, mirror_ready, mirror_focused, mirror_wait_for_scan;
static volatile uint8_t mirror_request;
static volatile uint8_t mirror_full_request;

static uint16_t *mirror_pixels(unsigned offset)
{
    return offset < MIRROR_POOL_ROWS ? mirror_pool[offset] : mirror_extra[offset - MIRROR_POOL_ROWS];
}

static void mirror_clear(unsigned x, unsigned y, unsigned w, unsigned h, uint16_t color)
{
    if (x == 0 && y == 0 && w == MIRROR_WIDTH && h == MIRROR_WIDTH)
        mirror_background = color;
    if (!mirror_capturing) return;
    for (unsigned row = 0; row < mirror_height; ++row) {
        if (mirror_top + row < y || mirror_top + row >= y + h) continue;
        uint16_t *target = mirror_pixels(row);
        for (unsigned col = x; col < x + w; ++col)
            target[col] = color;
        if (x == 0 && w == MIRROR_WIDTH) mirror_covered |= 1u << row;
    }
}

static void mirror_blit(unsigned x, unsigned y, unsigned w, unsigned h, const uint16_t *pixels)
{
    if (!mirror_capturing || x >= MIRROR_WIDTH) return;
    const unsigned width = w > MIRROR_WIDTH - x ? MIRROR_WIDTH - x : w;
    for (unsigned row = 0; row < mirror_height; ++row) {
        const unsigned at = mirror_top + row;
        if (at < y || at >= y + h) continue;
        const uint16_t *source = pixels + (at - y) * w;
        uint16_t *target = mirror_pixels(row) + x;
        for (unsigned col = 0; col < width; ++col) {
            const uint16_t color = source[col];          /* LCD DMA uses big-endian RGB565 */
            target[col] = (uint16_t)(color >> 8 | color << 8);
        }
        if (x == 0 && width == MIRROR_WIDTH) mirror_covered |= 1u << row;
    }
}

static void mirror_focus(unsigned top, unsigned height, uint32_t now)
{
    /* Keep progressing through the rest of the screen during a long turn. */
    if (!sm_on() || mirror_focus_left || mirror_wait_for_scan || now - mirror_focus_time < 150u) return;
    mirror_focus_top = top;
    mirror_focus_left = height;
    mirror_focus_time = now;
}

static unsigned mirror_next(void)
{
    return mirror_focus_left ? mirror_focus_top : mirror_scan;
}

static int mirror_begin(unsigned limit)
{
    if (!sm_on() || mirror_ready || mirror_capturing) return 0;
    mirror_top = mirror_next();
    mirror_height = limit - mirror_top;
    if (mirror_height > MIRROR_BAND) mirror_height = MIRROR_BAND;
    mirror_focused = mirror_focus_left != 0;
    if (mirror_focused && mirror_height > mirror_focus_left) mirror_height = mirror_focus_left;
    mirror_capturing = 1;
    mirror_clear(0, mirror_top, MIRROR_WIDTH, mirror_height, mirror_background);
    mirror_covered = 0;
    return 1;
}

static void mirror_finish(void)
{
    if (!mirror_capturing) return;
    mirror_capturing = 0;
    /* A renderer can defer a blit (e.g. a visualizer's frame cadence). Keep
     * the old screen until we have a real, complete band. */
    if (mirror_covered != (1u << mirror_height) - 1u) return;
    mirror_ready = 1;
    mirror_offset = mirror_row = mirror_dirty = 0;
}

static void mirror_reset(void)
{
    mirror_ready = mirror_capturing = 0;
    mirror_scan = mirror_focus_left = mirror_wait_for_scan = 0;
    for (unsigned i = 0; i < MIRROR_TILES; ++i) mirror_hashes[i] = 0;
}

static void mirror_next_group(void)
{
    mirror_offset += MIRROR_TILE_H;
    if (mirror_offset < mirror_height) return;
    mirror_ready = 0;
    if (mirror_focused) {
        mirror_focus_top += mirror_height;
        mirror_focus_left -= mirror_height;
        if (!mirror_focus_left) mirror_wait_for_scan = 1;
    } else {
        mirror_wait_for_scan = 0;
        mirror_scan = mirror_top + mirror_height;
        if (mirror_scan == MIRROR_WIDTH) mirror_scan = 0;
    }
}

static unsigned mirror_diff(unsigned first)
{
    unsigned dirty = 0;
    for (unsigned col = 0; col < MIRROR_COLS; ++col) {
        uint32_t hash = 2166136261u;
        for (unsigned row = 0; row < MIRROR_TILE_H; ++row) {
            const uint16_t *pixels = mirror_pixels(mirror_offset + row) + col * MIRROR_TILE_W;
            for (unsigned i = 0; i < MIRROR_TILE_W; ++i)
                hash = (hash ^ pixels[i]) * 16777619u;
        }
        mirror_pending[col] = hash ? hash : 1u;
        if (mirror_hashes[first + col] != mirror_pending[col]) dirty |= 1u << col;
    }
    return dirty;
}

/* Stage one complete row segment per main-loop turn. Join adjoining dirty
 * tiles so a full screen uses the original packets, without per-tile headers. */
static void mirror_stage(void)
{
    if (!mirror_ready) return;
    const unsigned index = (mirror_top + mirror_offset) / MIRROR_TILE_H * MIRROR_COLS;
    if (!mirror_dirty) {
        mirror_dirty = mirror_diff(index);
        if (!mirror_dirty) {
            mirror_next_group();
            return;
        }
    }
    if (!mirror_row) {
        mirror_first = 0;
        while (!(mirror_dirty & (1u << mirror_first))) ++mirror_first;
        mirror_span = 1;
        while (mirror_first + mirror_span < MIRROR_COLS &&
               (mirror_dirty & (1u << (mirror_first + mirror_span)))) ++mirror_span;
    }
    const unsigned x = mirror_first * MIRROR_TILE_W;
    const unsigned width = mirror_span * MIRROR_TILE_W;
    const unsigned max_bytes = (width + 14u) / 15u * (SM_MESSAGE + 1u);
    if (sm_length > SM_BUFFER - max_bytes) return;
    const unsigned row = mirror_offset + mirror_row;
    if (!sm_rows(mirror_pixels(row) + x, x, mirror_top + row, width, 1)) return;
    if (++mirror_row == MIRROR_TILE_H) {
        for (unsigned col = mirror_first; col < mirror_first + mirror_span; ++col) {
            mirror_hashes[index + col] = mirror_pending[col];
            mirror_dirty &= ~(1u << col);
        }
        mirror_row = 0;
        if (!mirror_dirty) mirror_next_group();
    }
}
