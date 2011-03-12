package org.jruby.ext.oily_png;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import static org.jruby.ext.oily_png.oily_png_ext_c.*;
import static org.jruby.ext.oily_png.png_decoding_c.*;

public class PNGDecoding {
    @JRubyMethod(required = 6)
    public static IRubyObject oily_png_decode_png_image_pass(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        ByteList stream = args[0].convertToString().getByteList();
        IRubyObject widthObj = args[1];
        int width = RubyNumeric.fix2int(widthObj);
        IRubyObject heightObj = args[2];
        int height = RubyNumeric.fix2int(heightObj);
        int color_mode = RubyNumeric.fix2int(args[3]);
        int depth = RubyNumeric.fix2int(args[4]);
        int start_pos = RubyNumeric.fix2int(args[5]);

        RubyArray pixels = runtime.newEmptyArray();

        if (height > 0 && width > 0) {

            int pixel_size = oily_png_pixel_bytesize(runtime, color_mode, depth);
            int line_size = oily_png_scanline_bytesize(runtime, color_mode, depth, width);
            int pass_size = oily_png_pass_bytesize(runtime, color_mode, depth, width, height);

            // Make sure that the stream is large enough to contain our pass.
            if (stream.getRealSize() < pass_size + start_pos) {
                throw runtime.newRuntimeError("The length of the stream is too short to contain the image!");
            }

            // Copy the bytes for this pass from the stream to a separate location
            // so we can work on this byte array directly.
            byte[] bytes = new byte[pass_size];
            System.arraycopy(stream.getUnsafeBytes(), stream.getBegin() + start_pos, bytes, 0, pass_size);

            // Get the decoding palette for indexed images.
            RubyArray decoding_palette = null;
            if (color_mode == OILY_PNG_COLOR_INDEXED) {
                decoding_palette = oily_png_decode_palette(self);
            }

            // Select the scanline decoder function for this color mode and bit depth.
            // TODO
//            scanline_decoder_func scanline_decoder = oily_png_decode_scanline_func(color_mode, depth);
//            if (scanline_decoder == null) {
//                throw runtime.newRuntimeError("No decoder for color mode " + color_mode + " and bit depth " + depth);
//            }

            int y, line_start;
            for (y = 0; y < height; y++) {
                line_start = y * line_size;

                // Apply filering to the line
                switch (bytes[line_start]) {
                case OILY_PNG_FILTER_NONE:
                    break;
                case OILY_PNG_FILTER_SUB:
                    oily_png_decode_filter_sub(bytes, line_start, line_size, pixel_size);
                    break;
                case OILY_PNG_FILTER_UP:
                    oily_png_decode_filter_up(bytes, line_start, line_size, pixel_size);
                    break;
                case OILY_PNG_FILTER_AVERAGE:
                    oily_png_decode_filter_average(bytes, line_start, line_size, pixel_size);
                    break;
                case OILY_PNG_FILTER_PAETH:
                    oily_png_decode_filter_paeth(bytes, line_start, line_size, pixel_size);
                    break;
                default:
                    runtime.newRuntimeError("Filter type not supported: " + bytes[line_start]);
                }

                // Set the filter byte to 0 because the bytearray is now unfiltered.
                bytes[line_start] = OILY_PNG_FILTER_NONE;
                // TODO
//                scanline_decoder(pixels, bytes, line_start, width, decoding_palette);
            }
        }

        // Now, return a new ChunkyPNG::Canvas instance with the decoded pixels.
        return self.callMethod(context, "new", new IRubyObject[]{widthObj, heightObj, pixels});
    }
}
