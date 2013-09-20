package org.jruby.ext.oily_png;

import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyNumeric;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.ext.oily_png.color_c.*;
import static org.jruby.ext.oily_png.oily_png_ext_c.*;

class png_encoding_c {

    static byte FILTER_BYTE(byte bite, int adjustment) {
        return (byte) (((bite) - (adjustment)) & 0x000000ff);
    }

    static int ENCODING_PALETTE_INDEX(ThreadContext context, RubyHash encoding_palette, RubyArray pixels, int width, int y, int x) {
        return ((x) < (width))
                ? RubyNumeric.num2int(encoding_palette.op_aref(context, pixels.entry((y) * (width) + (x))))
                : 0;
    }

    static void oily_png_encode_filter_sub(byte[] bytes, int offset, int pos, int line_size, char pixel_size) {
        int x;
        for (x = line_size - 1; x > pixel_size; x--) {
            FILTER_BYTE(bytes[offset + pos + x], bytes[offset + pos + x - pixel_size]);
        }
    }

    static void oily_png_encode_filter_up(byte[] bytes, int offset, int pos, int line_size, char pixel_size) {
        int x;
        if (pos >= line_size) {
            for (x = line_size - 1; x > 0; x--) {
                FILTER_BYTE(bytes[offset + pos + x], bytes[offset + pos + x - line_size]);
            }
        }
    }

    static void oily_png_encode_filter_average(byte[] bytes, int offset, int pos, int line_size, char pixel_size) {
        int x;
        byte a, b;
        for (x = line_size - 1; x > 0; x--) {
            a = (x > pixel_size) ? bytes[offset + pos + x - pixel_size] : 0;
            b = (pos >= line_size) ? bytes[offset + pos + x - line_size] : 0;
            FILTER_BYTE(bytes[offset + pos + x], (a + b) >> 1);
        }
    }

    static void oily_png_encode_filter_paeth(byte[] bytes, int offset, int pos, int line_size, char pixel_size) {
        int x;
        int p, pa, pb, pc;
        byte a, b, c, pr;
        for (x = line_size - 1; x > 0; x--) {
            a = (x > pixel_size) ? bytes[offset + pos + x - pixel_size] : 0;
            b = (pos >= line_size) ? bytes[offset + pos + x - line_size] : 0;
            c = (pos >= line_size && x > pixel_size) ? bytes[offset + pos + x - line_size - pixel_size] : 0;
            p = a + b - c;
            pa = Math.abs(p - a);
            pb = Math.abs(p - b);
            pc = Math.abs(p - c);
            pr = (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c);
            FILTER_BYTE(bytes[offset + pos + x], pr);
        }
    }

    ///// Scanline encoding functions //////////////////////////////////////////
    // Assume R == G == B. ChunkyPNG uses the B byte fot performance reasons. 
    // We'll uses the same to remain compatible with ChunkyPNG.
    static void oily_png_encode_scanline_grayscale_1bit(byte[] bytes, int offset, IRubyObject _pixels, int y, int width, IRubyObject encoding_palette) {
        RubyArray pixels = (RubyArray) _pixels;
        int x;
        byte p1, p2, p3, p4, p5, p6, p7, p8;
        for (x = 0; x < width; x += 8) {
            p1 = (x + 0 >= width) ? (byte) 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 0))) >> 7);
            p2 = (x + 1 >= width) ? 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 1))) >> 7);
            p3 = (x + 2 >= width) ? 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 2))) >> 7);
            p4 = (x + 3 >= width) ? 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 3))) >> 7);
            p5 = (x + 4 >= width) ? 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 4))) >> 7);
            p6 = (x + 5 >= width) ? 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 5))) >> 7);
            p7 = (x + 6 >= width) ? 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 6))) >> 7);
            p8 = (x + 7 >= width) ? 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 7))) >> 7);
            bytes[offset + x >> 3] = (byte) ((p1 << 7) | (p2 << 6) | (p3 << 5) | (p4 << 4) | (p5 << 3) | (p6 << 2) | (p7 << 1) | (p8));
        }
    }

    // Assume R == G == B. ChunkyPNG uses the B byte fot performance reasons. 
    // We'll uses the same to remain compatible with ChunkyPNG.
    static void oily_png_encode_scanline_grayscale_2bit(byte[] bytes, int offset, IRubyObject _pixels, int y, int width, IRubyObject encoding_palette) {
        RubyArray pixels = (RubyArray) _pixels;
        int x;
        byte p1, p2, p3, p4;
        for (x = 0; x < width; x += 4) {
            p1 = (x + 0 >= width) ? 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 0))) >> 6);
            p2 = (x + 1 >= width) ? 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 1))) >> 6);
            p3 = (x + 2 >= width) ? 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 2))) >> 6);
            p4 = (x + 3 >= width) ? 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 3))) >> 6);
            bytes[offset + x >> 2] = (byte) ((p1 << 6) | (p2 << 4) | (p3 << 2) | (p4));
        }
    }

// Assume R == G == B. ChunkyPNG uses the B byte fot performance reasons. 
// We'll uses the same to remain compatible with ChunkyPNG.
    static void oily_png_encode_scanline_grayscale_4bit(byte[] bytes, int offset, IRubyObject _pixels, int y, int width, IRubyObject encoding_palette) {
        RubyArray pixels = (RubyArray) _pixels;
        int x;
        byte p1, p2;
        for (x = 0; x < width; x += 2) {
            p1 = (x + 0 >= width) ? 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 0))) >> 4);
            p2 = (x + 1 >= width) ? 0 : (byte) ((RubyNumeric.num2int(pixels.eltInternal(y * width + x + 1))) >> 4);
            bytes[offset + x >> 1] = (byte) ((p1 << 4) | (p2));
        }
    }

// Assume R == G == B. ChunkyPNG uses the B byte fot performance reasons. 
// We'll uses the same to remain compatible with ChunkyPNG.
    static void oily_png_encode_scanline_grayscale_8bit(byte[] bytes, int offset, IRubyObject _pixels, int y, int width, IRubyObject encoding_palette) {
        RubyArray pixels = (RubyArray) _pixels;
        int x;
        int pixel;
        for (x = 0; x < width; x++) {
            pixel = RubyNumeric.num2int(pixels.eltInternal(y * width + x));
            bytes[offset + x] = B_BYTE(pixel);
        }
    }

// Assume R == G == B. ChunkyPNG uses the B byte fot performance reasons. 
// We'll uses the same to remain compatible with ChunkyPNG.
    static void oily_png_encode_scanline_grayscale_alpha_8bit(byte[] bytes, int offset, IRubyObject _pixels, int y, int width, IRubyObject encoding_palette) {
        RubyArray pixels = (RubyArray) _pixels;
        int x;
        int pixel;
        for (x = 0; x < width; x++) {
            pixel = RubyNumeric.num2int(pixels.eltInternal(y * width + x));
            bytes[offset + x * 2 + 0] = B_BYTE(pixel);
            bytes[offset + x * 2 + 1] = A_BYTE(pixel);
        }
    }

    static void oily_png_encode_scanline_indexed_8bit(byte[] bytes, int offset, IRubyObject _pixels, int y, int width, IRubyObject encoding_palette) {
        RubyArray pixels = (RubyArray) _pixels;
        int x;
        for (x = 0; x < width; x++) {
            bytes[offset + x] = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x);
        }
    }

    static void oily_png_encode_scanline_indexed_4bit(byte[] bytes, int offset, IRubyObject _pixels, int y, int width, IRubyObject encoding_palette) {
        RubyArray pixels = (RubyArray) _pixels;
        int x;
        byte p1, p2;
        for (x = 0; x < width; x += 2) {
            p1 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 0);
            p2 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 1);
            bytes[offset + x >> 1] = (byte) ((p1 << 4) | (p2));
        }
    }

    static void oily_png_encode_scanline_indexed_2bit(byte[] bytes, int offset, IRubyObject _pixels, int y, int width, IRubyObject encoding_palette) {
        RubyArray pixels = (RubyArray) _pixels;
        int x;
        byte p1, p2, p3, p4;
        for (x = 0; x < width; x += 4) {
            p1 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 0);
            p2 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 1);
            p3 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 2);
            p4 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 3);
            bytes[offset + x >> 2] = (byte) ((p1 << 6) | (p2 << 4) | (p3 << 2) | (p4));
        }
    }

    static void oily_png_encode_scanline_indexed_1bit(byte[] bytes, int offset, IRubyObject _pixels, int y, int width, IRubyObject encoding_palette) {
        RubyArray pixels = (RubyArray) _pixels;
        int x;
        byte p1, p2, p3, p4, p5, p6, p7, p8;
        for (x = 0; x < width; x += 8) {
            p1 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 0);
            p2 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 1);
            p3 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 2);
            p4 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 3);
            p5 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 4);
            p6 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 5);
            p7 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 6);
            p8 = (byte) ENCODING_PALETTE_INDEX(pixels.getRuntime().getCurrentContext(), (RubyHash) encoding_palette, pixels, width, y, x + 7);
            bytes[offset + x >> 3] = (byte) ((p1 << 7) | (p2 << 6) | (p3 << 5) | (p4 << 4) | (p5 << 3) | (p6 << 2) | (p7 << 1) | (p8));
        }
    }

    static void oily_png_encode_scanline_truecolor_8bit(byte[] bytes, int offset, IRubyObject _pixels, int y, int width, IRubyObject encoding_palette) {
        RubyArray pixels = (RubyArray) _pixels;
        int x;
        int pixel;
        for (x = 0; x < width; x++) {
            pixel = RubyNumeric.num2int(pixels.eltInternal(y * width + x));
            bytes[offset + x * 3 + 0] = R_BYTE(pixel);
            bytes[offset + x * 3 + 1] = G_BYTE(pixel);
            bytes[offset + x * 3 + 2] = B_BYTE(pixel);
        }
    }

    static void oily_png_encode_scanline_truecolor_alpha_8bit(byte[] bytes, int offset, IRubyObject _pixels, int y, int width, IRubyObject encoding_palette) {
        RubyArray pixels = (RubyArray) _pixels;
        int x;
        int pixel;
        for (x = 0; x < width; x++) {
            pixel = RubyNumeric.num2int(pixels.eltInternal(y * width + x));
            bytes[offset + x * 4 + 0] = R_BYTE(pixel);
            bytes[offset + x * 4 + 1] = G_BYTE(pixel);
            bytes[offset + x * 4 + 2] = B_BYTE(pixel);
            bytes[offset + x * 4 + 3] = A_BYTE(pixel);
        }
    }

    interface scanline_encoder_func {

        void call(byte[] b, int offset, IRubyObject v1, int i1, int i2, IRubyObject v2);
    }

    static scanline_encoder_func oily_png_encode_scanline_func(char color_mode, char bit_depth) {
        switch (color_mode) {

            case OILY_PNG_COLOR_GRAYSCALE:
                switch (bit_depth) {
                    case 8:
                        return new scanline_encoder_func() {
                            public void call(byte[] b, int offset, IRubyObject v1, int i1, int i2, IRubyObject v2) {
                                oily_png_encode_scanline_grayscale_8bit(b, offset, v1, i1, i2, v2);
                            }
                        };
                    case 4:
                        return new scanline_encoder_func() {
                            public void call(byte[] b, int offset, IRubyObject v1, int i1, int i2, IRubyObject v2) {
                                oily_png_encode_scanline_grayscale_4bit(b, offset, v1, i1, i2, v2);
                            }
                        };
                    case 2:
                        return new scanline_encoder_func() {
                            public void call(byte[] b, int offset, IRubyObject v1, int i1, int i2, IRubyObject v2) {
                                oily_png_encode_scanline_grayscale_2bit(b, offset, v1, i1, i2, v2);
                            }
                        };
                    case 1:
                        return new scanline_encoder_func() {
                            public void call(byte[] b, int offset, IRubyObject v1, int i1, int i2, IRubyObject v2) {
                                oily_png_encode_scanline_grayscale_1bit(b, offset, v1, i1, i2, v2);
                            }
                        };
                    default:
                        return null;
                }

            case OILY_PNG_COLOR_GRAYSCALE_ALPHA:
                switch (bit_depth) {
                    case 8:
                        return new scanline_encoder_func() {
                            public void call(byte[] b, int offset, IRubyObject v1, int i1, int i2, IRubyObject v2) {
                                oily_png_encode_scanline_grayscale_alpha_8bit(b, offset, v1, i1, i2, v2);
                            }
                        };
                    default:
                        return null;
                }

            case OILY_PNG_COLOR_INDEXED:
                switch (bit_depth) {
                    case 8:
                        return new scanline_encoder_func() {
                            public void call(byte[] b, int offset, IRubyObject v1, int i1, int i2, IRubyObject v2) {
                                oily_png_encode_scanline_indexed_8bit(b, offset, v1, i1, i2, v2);
                            }
                        };
                    case 4:
                        return new scanline_encoder_func() {
                            public void call(byte[] b, int offset, IRubyObject v1, int i1, int i2, IRubyObject v2) {
                                oily_png_encode_scanline_indexed_4bit(b, offset, v1, i1, i2, v2);
                            }
                        };
                    case 2:
                        return new scanline_encoder_func() {
                            public void call(byte[] b, int offset, IRubyObject v1, int i1, int i2, IRubyObject v2) {
                                oily_png_encode_scanline_indexed_2bit(b, offset, v1, i1, i2, v2);
                            }
                        };
                    case 1:
                        return new scanline_encoder_func() {
                            public void call(byte[] b, int offset, IRubyObject v1, int i1, int i2, IRubyObject v2) {
                                oily_png_encode_scanline_indexed_1bit(b, offset, v1, i1, i2, v2);
                            }
                        };
                    default:
                        return null;
                }

            case OILY_PNG_COLOR_TRUECOLOR:
                switch (bit_depth) {
                    case 8:
                        return new scanline_encoder_func() {
                            public void call(byte[] b, int offset, IRubyObject v1, int i1, int i2, IRubyObject v2) {
                                oily_png_encode_scanline_truecolor_8bit(b, offset, v1, i1, i2, v2);
                            }
                        };
                    default:
                        return null;
                }

            case OILY_PNG_COLOR_TRUECOLOR_ALPHA:
                switch (bit_depth) {
                    case 8:
                        return new scanline_encoder_func() {
                            public void call(byte[] b, int offset, IRubyObject v1, int i1, int i2, IRubyObject v2) {
                                oily_png_encode_scanline_truecolor_alpha_8bit(b, offset, v1, i1, i2, v2);
                            }
                        };
                    default:
                        return null;
                }

            default:
                return null;
        }
    }
}
