package org.jruby.ext.oily_png;

import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyNumeric;
import org.jruby.runtime.ThreadContext;

class png_encoding_c {
static byte FILTER_BYTE(byte bite, int adjustment) {
    return (byte) (((bite) - (adjustment)) & 0x000000ff);
}
static int ENCODING_PALETTE_INDEX(ThreadContext context, RubyHash encoding_palette, RubyArray pixels, int width, int y, int x) {
    return ((x) < (width)) ?
        RubyNumeric.num2int(encoding_palette.op_aref(context, pixels.entry((y) * (width) + (x)))) :
        0;
    }
}
