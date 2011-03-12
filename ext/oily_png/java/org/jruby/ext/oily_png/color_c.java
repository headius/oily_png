package org.jruby.ext.oily_png;

class color_c {
    static byte R_BYTE(long pixel) {return (byte)((pixel & 0xff000000) >> 24);}
    static byte G_BYTE(long pixel) {return (byte)((pixel & 0xff0000) >> 16);}
    static byte B_BYTE(long pixel) {return (byte)((pixel & 0xff00) >> 8);}
    static byte A_BYTE(long pixel) {return (byte)(pixel & 0xff);}

    static long BUILD_PIXEL(long r, long g, long b, long a) {
        return ((r << 24) + (g << 16) + (b << 8) + a);
    }
    /*
#define INT8_MULTIPLY(a, b)      (((((a) * (b) + 0x80) >> 8) + ((a) * (b) + 0x80)) >> 8)
     */
}