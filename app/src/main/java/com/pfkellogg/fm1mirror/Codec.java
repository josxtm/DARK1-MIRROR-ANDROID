/* Adapted from FM-1 Mirror/Spyglass by Patrick F. Kellogg. See LICENSE. */
package com.pfkellogg.fm1mirror;

final class Codec {
    private Codec() {}

    static final int SIZE = 240;
    static final byte[] HELLO = {(byte) 0xf0, 0x7d, 'F', 'S', 0, (byte) 0xf7};
    static final byte[] HELLO_ALL = {(byte) 0xf0, 0x7d, 'F', 'S', 1, (byte) 0xf7};
    static final byte[] ASK_INFO = {(byte) 0xf0, 0x7d, 'F', 'L', 1, (byte) 0xf7};

    /** The firmware's own name, if this is its answer to ASK_INFO: "FELUCCA SLOOP 2.8.4", then other things. */
    static String firmwareInfo(byte[] m) {
        int n = m.length;
        if (n < 7 || m[0] != (byte) 0xf0 || m[1] != 0x7d || m[2] != 'F' || !((m[3] == 'L' && m[4] == 1) || (m[3] == 'S' && m[4] == 3)) || m[n - 1] != (byte) 0xf7)
            return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 5; i < n - 1 && m[i] != 0; i++)
            if (m[i] >= 0x20 && m[i] < 0x7f) sb.append((char) m[i]);
        String name = sb.toString().trim();
        return name.isEmpty() ? null : name;
    }

    /** The name to show. SLOOP and Skiff keep Felucca's USB name, and their answers start with "FELUCCA " for the web
     *  tools: "FELUCCA SLOOP 2.8.4" is SLOOP, and "FELUCCA v2.1.1" is Skiff when its identity is FM-1_7xx, else Felucca. */
    static String firmwareName(String info, String identity) {
        if (!info.startsWith("FELUCCA ")) return info;
        String rest = info.substring(8);
        boolean bareVersion = rest.length() > 1 && rest.charAt(0) == 'v' && Character.isDigit(rest.charAt(1));
        if (!bareVersion) return rest;
        return (identity != null && identity.startsWith("FM-1_7") ? "Skiff " : "Felucca ") + rest;
    }

    /** Paints one row message into `pixels` (RGB565 to opaque ARGB); false if it isn't one. */
    static boolean apply(int[] pixels, byte[] m) {
        int n = m.length;
        if (n < 10 || m[0] != (byte) 0xf0 || m[1] != 0x7d || m[2] != 'F' || m[3] != 'S' || m[4] != 1 || m[n - 1] != (byte) 0xf7)
            return false;
        int y = (m[5] & 127) | (m[6] & 127) << 7, x = (m[7] & 127) | (m[8] & 127) << 7;
        if (y >= SIZE) return false;
        for (int i = 9; i + 4 <= n - 1; i += 4) {
            int c = (m[i + 1] & 127) | (m[i + 2] & 127) << 7 | (m[i + 3] & 127) << 14;
            int color = argb(c);
            for (int k = 0; k < (m[i] & 127); k++, x++)
                if (x < SIZE) pixels[y * SIZE + x] = color;
        }
        return true;
    }

    /** RGB565 to 8-bit red, green, blue. */
    static int argb(int c) {
        int r = (c >> 11) & 31, g = (c >> 5) & 63, b = c & 31;
        return 0xff000000 | (r << 3 | r >> 2) << 16 | (g << 2 | g >> 4) << 8 | (b << 3 | b >> 2);
    }

}
