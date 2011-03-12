package org.jruby.ext.oily_png;

import org.jruby.Ruby;

class oily_png_ext_c {
    // PNG color mode constants
    static final int OILY_PNG_COLOR_GRAYSCALE = 0;
    static final int OILY_PNG_COLOR_TRUECOLOR = 2;
    static final int OILY_PNG_COLOR_INDEXED = 3;
    static final int OILY_PNG_COLOR_GRAYSCALE_ALPHA = 4;
    static final int OILY_PNG_COLOR_TRUECOLOR_ALPHA = 6;
    // PNG filter constants
    static final int OILY_PNG_FILTER_NONE = 0;
    static final int OILY_PNG_FILTER_SUB = 1;
    static final int OILY_PNG_FILTER_UP = 2;
    static final int OILY_PNG_FILTER_AVERAGE = 3;
    static final int OILY_PNG_FILTER_PAETH = 4;

    static int oily_png_samples_per_pixel(Ruby runtime, int color_mode) {
        switch (color_mode) {
        case OILY_PNG_COLOR_GRAYSCALE:
            return 1;
        case OILY_PNG_COLOR_TRUECOLOR:
            return 3;
        case OILY_PNG_COLOR_INDEXED:
            return 1;
        case OILY_PNG_COLOR_GRAYSCALE_ALPHA:
            return 2;
        case OILY_PNG_COLOR_TRUECOLOR_ALPHA:
            return 4;
        default:
            throw runtime.newRuntimeError("Unsupported color mode: " + color_mode);
        }
    }

    static int oily_png_pixel_bitsize(Ruby runtime, int color_mode, int bit_depth) {
        return oily_png_samples_per_pixel(runtime, color_mode) * bit_depth;
    }

    static int oily_png_pixel_bytesize(Ruby runtime, int color_mode, int bit_depth) {
        return (bit_depth < 8) ? 1 : (oily_png_pixel_bitsize(runtime, color_mode, bit_depth) + 7) >> 3;
    }

    static int oily_png_scanline_bytesize(Ruby runtime, int color_mode, int bit_depth, int width) {
        return (8 + ((oily_png_pixel_bitsize(runtime, color_mode, bit_depth) * width) + 7)) >> 3;
    }

    static int oily_png_pass_bytesize(Ruby runtime, int color_mode, int bit_depth, int width, int height) {
        return (width == 0 || height == 0) ? 0 : (oily_png_scanline_bytesize(runtime, color_mode, bit_depth, width)) * height;
    }
}
