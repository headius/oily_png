package org.jruby.ext.oily_png;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.ext.oily_png.oily_png_ext_c.*;
import static org.jruby.ext.oily_png.color_c.*;

class png_decoding_c {
    static void ADD_PIXEL_FROM_PALLETE(RubyArray pixels, RubyArray decoding_palette, int palette_entry) {
        if (decoding_palette.size() > palette_entry) {
            pixels.append(decoding_palette.entry(palette_entry));
        } else {
            throw pixels.getRuntime().newRuntimeError("The decoding palette does not have " + palette_entry + " entries!");
        }
    }

    static void ADD_PIXEL_FROM_RGBA(RubyArray pixels, long r, long g, long b, long a) {
        pixels.append(pixels.getRuntime().newFixnum(BUILD_PIXEL(r, g, b, a)));
    }
    /////////////////////////////////////////////////////////////////////
    // UNFILTERING SCANLINES
    /////////////////////////////////////////////////////////////////////
    // Decodes a SUB filtered scanline at the given position in the byte array

    static final void oily_png_decode_filter_sub(byte[] bytes, int pos, int line_length, int pixel_size) {
        int i;
        for (i = 1 + pixel_size; i < line_length; i++) {
            bytes[pos + i] = UNFILTER_BYTE(bytes[pos + i], bytes[pos + i - pixel_size]);
        }
    }
    // Decodes an UP filtered scanline at the given position in the byte array

    static final void oily_png_decode_filter_up(byte[] bytes, int pos, int line_size, int pixel_size) {

        int i;
        if (pos >= line_size) { // The first line is not filtered because there is no privous line
            for (i = 1; i < line_size; i++) {
                bytes[pos + i] = UNFILTER_BYTE(bytes[pos + i], bytes[pos + i - line_size]);
            }
        }
    }
    // Decodes an AVERAGE filtered scanline at the given position in the byte array

    static final void oily_png_decode_filter_average(byte[] bytes, int pos, int line_size, int pixel_size) {
        int i;
        byte a, b;
        for (i = 1; i < line_size; i++) {
            a = (i > pixel_size) ? bytes[pos + i - pixel_size] : 0;
            b = (pos >= line_size) ? bytes[pos + i - line_size] : 0;
            bytes[pos + i] = UNFILTER_BYTE(bytes[pos + i], (a + b) >> 1);
        }
    }
    // Decodes a PAETH filtered scanline at the given position in the byte array

    static void oily_png_decode_filter_paeth(byte[] bytes, int pos, int line_size, int pixel_size) {
        int a, b, c, pr;
        int i, p, pa, pb, pc;
        for (i = 1; i < line_size; i++) {
            a = (i > pixel_size) ? bytes[pos + i - pixel_size] : 0;
            b = (pos >= line_size) ? bytes[pos + i - line_size] : 0;
            c = (pos >= line_size && i > pixel_size) ? bytes[pos + i - line_size - pixel_size] : 0;
            p = a + b - c;
            pa = (p > a) ? p - a : a - p;
            pb = (p > b) ? p - b : b - p;
            pc = (p > c) ? p - c : c - p;
            pr = (pa <= pb) ? (pa <= pc ? a : c) : (pb <= pc ? b : c);
            bytes[pos + i] = UNFILTER_BYTE(bytes[pos + i], pr);
        }
    }

    static byte UNFILTER_BYTE(byte bite, int adjustment) {
        return (byte)((bite + adjustment) & 0xff);
    }
    /////////////////////////////////////////////////////////////////////
    // BIT HANDLING
    /////////////////////////////////////////////////////////////////////

    static final byte oily_png_extract_1bit_element(byte[] bytes, int start, int x) {
        byte bite = bytes[start + 1 + (x >> 3)];
        int bitshift = 7 - (x & (byte)0x07);
        return (byte)((bite & (0x01 << bitshift)) >> bitshift);
    }

    static final byte oily_png_extract_2bit_element(byte[] bytes, int start, int x) {
        byte bite = bytes[start + 1 + (x >> 2)];
        int bitshift = (6 - ((x & (byte)0x03) << 1));
        return (byte)((bite & (0x03 << bitshift)) >> bitshift);
    }

    static final byte oily_png_extract_4bit_element(byte[] bytes, int start, int x) {
        return ((x & 0x01) == 0)
                ? (byte)((bytes[(start) + 1 + ((x) >> 1)] & (byte)0xf0) >> 4)
                : (byte)(bytes[(start) + 1 + ((x) >> 1)] & (byte)0x0f);
    }

    static final byte oily_png_resample_1bit_element(byte[] bytes, int start, int x) {
        byte value = oily_png_extract_1bit_element(bytes, start, x);
        return (byte)((value == 0) ? 0x00 : 0xff);
    }

    static final byte oily_png_resample_2bit_element(byte[] bytes, int start, int x) {
        switch (oily_png_extract_2bit_element(bytes, start, x)) {
        case 0x00:
            return 0x00;
        case 0x01:
            return 0x55;
        case 0x02:
            return (byte)0xaa;
        case 0x03:
        default:
            return (byte)0xff;
        }
    }

    static final byte oily_png_resample_4bit_element(byte[] bytes, int start, int x) {
        switch (oily_png_extract_4bit_element(bytes, start, x)) {
        case 0x00:
            return 0;
        case 0x01:
            return 17;
        case 0x02:
            return 34;
        case 0x03:
            return 51;
        case 0x04:
            return 68;
        case 0x05:
            return 85;
        case 0x06:
            return 102;
        case 0x07:
            return 119;
        case 0x08:
            return (byte)137;
        case 0x09:
            return (byte)154;
        case 0x0a:
            return (byte)171;
        case 0x0b:
            return (byte)188;
        case 0x0c:
            return (byte)205;
        case 0x0d:
            return (byte)222;
        case 0x0e:
            return (byte)239;
        case 0x0f:
        default:
            return (byte)255;
        }
    }
    /////////////////////////////////////////////////////////////////////
    // PIXEL DECODING SCANLINES
    /////////////////////////////////////////////////////////////////////

    static final void oily_png_decode_scanline_grayscale_1bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {

        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_RGBA(pixels,
                    oily_png_resample_1bit_element(bytes, start, x),
                    oily_png_resample_1bit_element(bytes, start, x),
                    oily_png_resample_1bit_element(bytes, start, x),
                    0xff);
        }
    }

    static final void oily_png_decode_scanline_grayscale_2bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {

        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_RGBA(pixels,
                    oily_png_resample_2bit_element(bytes, start, x),
                    oily_png_resample_2bit_element(bytes, start, x),
                    oily_png_resample_2bit_element(bytes, start, x),
                    0xff);
        }
    }

    static final void oily_png_decode_scanline_grayscale_4bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {

        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_RGBA(pixels,
                    oily_png_resample_4bit_element(bytes, start, x),
                    oily_png_resample_4bit_element(bytes, start, x),
                    oily_png_resample_4bit_element(bytes, start, x),
                    0xff);
        }
    }

    static final void oily_png_decode_scanline_grayscale_8bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {

        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_RGBA(pixels,
                    bytes[start + 1 + x],
                    bytes[start + 1 + x],
                    bytes[start + 1 + x],
                    0xff);
        }
    }

    static final void oily_png_decode_scanline_grayscale_16bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {

        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_RGBA(pixels,
                    bytes[start + 1 + (x * 2)],
                    bytes[start + 1 + (x * 2)],
                    bytes[start + 1 + (x * 2)],
                    0xff);
        }
    }

    static void oily_png_decode_scanline_grayscale_alpha_8bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {

        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_RGBA(pixels,
                    bytes[start + 1 + (x * 2) + 0],
                    bytes[start + 1 + (x * 2) + 0],
                    bytes[start + 1 + (x * 2) + 0],
                    bytes[start + 1 + (x * 2) + 1]);
        }
    }

    static void oily_png_decode_scanline_grayscale_alpha_16bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {

        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_RGBA(pixels,
                    bytes[start + 1 + (x * 4) + 0],
                    bytes[start + 1 + (x * 4) + 0],
                    bytes[start + 1 + (x * 4) + 0],
                    bytes[start + 1 + (x * 4) + 2]);
        }
    }

    static void oily_png_decode_scanline_indexed_1bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {
        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_PALLETE(pixels, decoding_palette, oily_png_extract_1bit_element(bytes, start, x));
        }
    }

    static void oily_png_decode_scanline_indexed_2bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {
        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_PALLETE(pixels, decoding_palette, oily_png_extract_2bit_element(bytes, start, x));
        }
    }

    static void oily_png_decode_scanline_indexed_4bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {
        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_PALLETE(pixels, decoding_palette, oily_png_extract_4bit_element(bytes, start, x));
        }
    }

    static void oily_png_decode_scanline_indexed_8bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {
        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_PALLETE(pixels, decoding_palette, bytes[start + 1 + x]);
        }
    }

    static void oily_png_decode_scanline_truecolor_8bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {

        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_RGBA(pixels,
                    bytes[start + 1 + (x * 3) + 0],
                    bytes[start + 1 + (x * 3) + 1],
                    bytes[start + 1 + (x * 3) + 2],
                    0xff);
        }
    }

    static void oily_png_decode_scanline_truecolor_16bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {

        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_RGBA(pixels,
                    bytes[start + 1 + (x * 6) + 0],
                    bytes[start + 1 + (x * 6) + 2],
                    bytes[start + 1 + (x * 6) + 4],
                    0xff);
        }
    }

    static void oily_png_decode_scanline_truecolor_alpha_8bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {

        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_RGBA(pixels,
                    bytes[start + 1 + (x * 4) + 0],
                    bytes[start + 1 + (x * 4) + 1],
                    bytes[start + 1 + (x * 4) + 2],
                    bytes[start + 1 + (x * 4) + 3]);
        }
    }

    static void oily_png_decode_scanline_truecolor_alpha_16bit(RubyArray pixels, byte[] bytes, int start, int width, RubyArray decoding_palette) {

        int x;
        for (x = 0; x < width; x++) {
            ADD_PIXEL_FROM_RGBA(pixels,
                    bytes[start + 1 + (x * 8) + 0],
                    bytes[start + 1 + (x * 8) + 2],
                    bytes[start + 1 + (x * 8) + 4],
                    bytes[start + 1 + (x * 8) + 6]);
        }
    }
    /* TODO
    scanline_decoder_func oily_png_decode_scanline_func(int color_mode, int bit_depth) {
    switch (color_mode) {
    case OILY_PNG_COLOR_GRAYSCALE:
    switch (bit_depth) {
    case 1:
    return &  oily_png_decode_scanline_grayscale_1bit;
    case 2:
    return &  oily_png_decode_scanline_grayscale_2bit;
    case 4:
    return &  oily_png_decode_scanline_grayscale_4bit;
    case 8:
    return &  oily_png_decode_scanline_grayscale_8bit;
    case 16:
    return &  oily_png_decode_scanline_grayscale_16bit;
    default:
    return NULL;
    }

    case OILY_PNG_COLOR_TRUECOLOR:
    switch (bit_depth) {
    case 8:
    return &  oily_png_decode_scanline_truecolor_8bit;
    case 16:
    return &  oily_png_decode_scanline_truecolor_16bit;
    default:
    return NULL;
    }

    case OILY_PNG_COLOR_INDEXED:
    switch (bit_depth) {
    case 1:
    return &  oily_png_decode_scanline_indexed_1bit;
    case 2:
    return &  oily_png_decode_scanline_indexed_2bit;
    case 4:
    return &  oily_png_decode_scanline_indexed_4bit;
    case 8:
    return &  oily_png_decode_scanline_indexed_8bit;
    default:
    return NULL;
    }

    case OILY_PNG_COLOR_GRAYSCALE_ALPHA:
    switch (bit_depth) {
    case 8:
    return &  oily_png_decode_scanline_grayscale_alpha_8bit;
    case 16:
    return &  oily_png_decode_scanline_grayscale_alpha_16bit;
    default:
    return NULL;
    }

    case OILY_PNG_COLOR_TRUECOLOR_ALPHA:
    switch (bit_depth) {
    case 8:
    return &  oily_png_decode_scanline_truecolor_alpha_8bit;
    case 16:
    return &  oily_png_decode_scanline_truecolor_alpha_16bit;
    default:
    return NULL;
    }

    default:
    return NULL;
    }
    }*/
    /////////////////////////////////////////////////////////////////////
    // DECODING AN IMAGE PASS
    /////////////////////////////////////////////////////////////////////

    static RubyArray oily_png_decode_palette(IRubyObject self) {
        Ruby runtime = self.getRuntime();
        IRubyObject palette_instance = self.callMethod(runtime.getCurrentContext(), "decoding_palette");
        if (!palette_instance.isNil()) {
            IRubyObject decoding_map = palette_instance.getInstanceVariables().getInstanceVariable("@decoding_map");
            if (decoding_map instanceof RubyArray) {
                return (RubyArray)decoding_map;
            }
        }
        throw self.getRuntime().newRuntimeError("Could not retrieve a decoding palette for this image!");
    }
}
