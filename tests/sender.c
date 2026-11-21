/* SPDX-License-Identifier: GPL-3.0-only */
#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include "dark1_mirror.c"

static FILE *wire;
static int blocked;

static int send_packet(const uint8_t *data, unsigned size)
{
    if (blocked) return -2;
    assert(size <= 70);
    assert(fwrite(data, 1, size, wire) == size);
    return 0;
}

int main(int argc, char **argv)
{
    const uint8_t hello[] = {0xf0, 0x7d, 'F', 'S', 1, 0xf7};
    uint16_t dma[MIRROR_WIDTH];
    assert(argc == 2);
    wire = fopen(argv[1], "wb");
    assert(wire);
    assert(sm_sysex(hello, sizeof hello) == 1);
    sm_poll(1, send_packet);
    assert(sm_take_full_request() == 1);
    mirror_clear(0, 0, 240, 240, 0x001f);
    for (unsigned top = 0; top < 240; top += MIRROR_BAND) {
        mirror_top = top;
        assert(mirror_begin());
        for (unsigned row = top; row < top + MIRROR_BAND; row++) {
            for (unsigned x = 0; x < 240; x++) {
                uint16_t color = x < 16 ? 0xf800 : (uint16_t)(x * 877 + row * 37);
                dma[x] = (uint16_t)(color << 8 | color >> 8);
            }
            mirror_blit(16, row, 224, 1, dma);
        }
        mirror_finish();
        for (unsigned row = 0; row < MIRROR_BAND; row++) {
            assert(sm_rows(mirror_pixels[row], 0, top + row, 240, 1) == 1);
            blocked = 1;
            sm_poll(2, send_packet);
            blocked = 0;
            sm_poll(3, send_packet);
        }
        mirror_ready = 0;
    }
    sm_poll(3001, send_packet);
    assert(!sm_on());
    assert(fclose(wire) == 0);
}
