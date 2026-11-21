/* SPDX-License-Identifier: GPL-3.0-only */
#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include "dark1_mirror.c"

static FILE *wire;
static int blocked;
static unsigned packets, bytes, last_x, last_y;

static int send_packet(const uint8_t *data, unsigned size)
{
    if (blocked) return -2;
    assert(size <= 70);
    if (wire) assert(fwrite(data, 1, size, wire) == size);
    packets++;
    bytes += size;
    last_y = data[5] + 128u * data[6];
    last_x = data[7] + 128u * data[8];
    return 0;
}

static void snapshot(unsigned limit, unsigned edited)
{
    uint16_t dma[MIRROR_WIDTH];
    assert(mirror_begin(limit));
    mirror_clear(0, mirror_top, 240, mirror_height, 0x001f);
    for (unsigned y = mirror_top; y < mirror_top + mirror_height; ++y) {
        for (unsigned x = 0; x < 224; ++x) {
            uint16_t color = x < 16 ? 0xf800 : (uint16_t)(x * 877 + y * 37);
            if (x == 16 && y == 205 && edited) color ^= 0x0f0fu;
            dma[x] = (uint16_t)(color << 8 | color >> 8);
        }
        mirror_blit(16, y, 224, 1, dma);
    }
    mirror_finish();
    assert(mirror_ready);
}

static void drain(void)
{
    assert(mirror_ready);
    for (unsigned tries = 0; mirror_ready || sm_length; ++tries) {
        assert(tries < 3000);
        mirror_stage();
        if (tries < 15) {                   /* retain a partially staged band while USB is busy */
            blocked = 1;
            sm_poll(2, send_packet);
            blocked = 0;
        } else {
            sm_poll(3, send_packet);
        }
    }
}

int main(int argc, char **argv)
{
    const uint8_t hello[] = {0xf0, 0x7d, 'F', 'S', 1, 0xf7};
    assert(argc == 2);
    wire = fopen(argv[1], "wb");
    assert(wire);
    assert(sm_sysex(hello, sizeof hello) == 1);
    sm_poll(1, send_packet);
    assert(sm_take_full_request() == 1);
    mirror_reset();

    do {
        const unsigned next = mirror_next();
        snapshot(next < 28 ? 28 : next < 172 ? 172 : 240, 0);
        drain();
    } while (mirror_next());
    assert(fclose(wire) == 0);
    wire = NULL;
    fprintf(stderr, "First full sweep: %u SysEx messages, %u bytes\n", packets, bytes);

    packets = bytes = 0;
    do {
        const unsigned next = mirror_next();
        snapshot(next < 28 ? 28 : next < 172 ? 172 : 240, 0);
        drain();
    } while (mirror_next());
    assert(packets == 0 && bytes == 0);

    /* An unchanged LCD region may skip its blit on this frame. Keep waiting
     * for it instead of sending our placeholder colour. */
    assert(mirror_begin(28));
    mirror_finish();
    assert(!mirror_ready && mirror_next() == 0);

    mirror_focus(204, 24, 200);
    assert(mirror_next() == 204);
    snapshot(240, 1);
    drain();
    assert(packets > 0 && packets <= 16 && last_x < 60 && last_y == 207);
    const unsigned edited_bytes = bytes;
    assert(mirror_next() == 216);
    snapshot(240, 1);
    drain();
    assert(mirror_next() == 0 && bytes == edited_bytes);
    mirror_focus(204, 24, 500);
    assert(mirror_next() == 0);             /* an active knob cannot starve the rest of the screen */
    snapshot(28, 0);
    drain();
    assert(mirror_next() == MIRROR_BAND);
    mirror_focus(204, 24, 501);
    assert(mirror_next() == 204);
    fprintf(stderr, "Unchanged sweep: 0 bytes. Edited knob tile: %u messages, %u bytes.\n", packets, bytes);

    sm_poll(3001, send_packet);
    assert(!sm_on());
}
