package org.jruby.ext.oily_png;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.ext.oily_png.oily_png_ext_c.*;
import static org.jruby.ext.oily_png.png_encoding_c.*;
import org.jruby.runtime.ThreadContext;

public class PNGEncoding {

    @JRubyMethod(module = true)
    public static IRubyObject oily_png_encode_palette(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.getRuntime();
        IRubyObject palette_instance = self.callMethod(context, "encoding_palette");
        if (palette_instance != context.nil) {
            IRubyObject encoding_map = palette_instance.getInstanceVariables().getInstanceVariable("@encoding_map");
            if (self.callMethod(context, "kind_of?", runtime.getHash()) == runtime.getTrue()) {
                return encoding_map;
            }
        }
        throw runtime.newRuntimeError("Could not retrieve a decoding palette for this image!");
    }

    interface scanline_filter_func {

        void call(byte[] b, int offset, int i1, int i2, char c);
    }

    @JRubyMethod(required = 5, module = true)
    public static IRubyObject oily_png_encode_png_image_pass_to_stream(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return oily_png_encode_png_image_pass_to_stream(context, self, args[0], args[1], args[2], args[3]);
    }
    
    static IRubyObject oily_png_encode_png_image_pass_to_stream(ThreadContext context, IRubyObject self, IRubyObject _stream, IRubyObject color_mode, IRubyObject bit_depth, IRubyObject filtering) {
        Ruby runtime = context.runtime;
        // Get the data
        char depth = (char) RubyNumeric.fix2int(bit_depth);
        int width = RubyNumeric.fix2int(self.callMethod(context, "width"));
        int height = RubyNumeric.fix2int(self.callMethod(context, "height"));
        RubyArray pixels = self.callMethod(context, "pixels").convertToArray();

        if (pixels.size() != width * height) {
            throw runtime.newRuntimeError("The number of pixels does not match the canvas dimensions.");
        }

        // Get the encoding palette if we're encoding to an indexed bytestream.
        IRubyObject encoding_palette = context.nil;
        if (RubyNumeric.fix2int(color_mode) == OILY_PNG_COLOR_INDEXED) {
            encoding_palette = oily_png_encode_palette(context, self);
        }

        char pixel_size = (char) oily_png_pixel_bytesize(runtime, RubyNumeric.fix2int(color_mode), depth);
        int line_size = oily_png_scanline_bytesize(runtime, RubyNumeric.fix2int(color_mode), depth, width);
        int pass_size = oily_png_pass_bytesize(runtime, RubyNumeric.fix2int(color_mode), depth, width, height);

        // Allocate memory for the byte array.
        byte[] bytes = new byte[pass_size];

        // Get the scanline encoder function.
        scanline_encoder_func scanline_encoder = oily_png_encode_scanline_func((char) RubyNumeric.fix2int(color_mode), depth);
        if (scanline_encoder == null) {
            throw runtime.newRuntimeError("No encoder for color mode " + RubyNumeric.fix2int(color_mode) + " and bit depth " + depth);
        }

        int y, pos;
        for (y = height - 1; y >= 0; y--) {
            pos = line_size * y;
            bytes[pos] = (byte) RubyNumeric.fix2int(filtering);
            scanline_encoder.call(bytes, pos + 1, pixels, y, width, encoding_palette);
        }

        if (RubyNumeric.fix2int(filtering) != OILY_PNG_FILTER_NONE) {

            // Get the scanline filter function
            scanline_filter_func scanline_filter = null;
            switch (RubyNumeric.fix2int(filtering)) {
                case OILY_PNG_FILTER_SUB:
                    scanline_filter = new scanline_filter_func() {
                        public void call(byte[] b, int offset, int i1, int i2, char c) {
                            oily_png_encode_filter_sub(b, offset, i1, i2, c);
                        }
                    };
                    break;
                case OILY_PNG_FILTER_UP:
                    scanline_filter = new scanline_filter_func() {
                        public void call(byte[] b, int offset, int i1, int i2, char c) {
                            oily_png_encode_filter_up(b, offset, i1, i2, c);
                        }
                    };
                    break;
                case OILY_PNG_FILTER_AVERAGE:
                    scanline_filter = new scanline_filter_func() {
                        public void call(byte[] b, int offset, int i1, int i2, char c) {
                            oily_png_encode_filter_average(b, offset, i1, i2, c);
                        }
                    };
                    break;
                case OILY_PNG_FILTER_PAETH:
                    scanline_filter = new scanline_filter_func() {
                        public void call(byte[] b, int offset, int i1, int i2, char c) {
                            oily_png_encode_filter_paeth(b, offset, i1, i2, c);
                        }
                    };
                    break;
                default:
                    throw runtime.newRuntimeError("Unsupported filter type: " + RubyNumeric.fix2int(filtering));
            }

            for (y = height - 1; y >= 0; y--) {
                scanline_filter.call(bytes, 0, line_size * y, line_size, pixel_size);
            }
        }

        // Append to encoded image pass to the output stream.
        RubyString stream = _stream.convertToString();
        stream.cat(bytes, 0, pass_size);
        return context.nil;
    }
}