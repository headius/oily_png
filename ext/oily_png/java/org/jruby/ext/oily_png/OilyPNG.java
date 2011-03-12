package org.jruby.ext.oily_png;

import org.jruby.Ruby;
import org.jruby.RubyModule;

public class OilyPNG {
    public static void load(Ruby runtime) {
        RubyModule oilyPng = runtime.defineModule("OilyPNG");
        RubyModule pngDecoding = oilyPng.defineModuleUnder("PNGDecoding");
        RubyModule pngEncoding = oilyPng.defineModuleUnder("PNGEncoding");
        RubyModule color = oilyPng.defineModuleUnder("Color");
        RubyModule operations = oilyPng.defineModuleUnder("Operations");

        pngDecoding.defineAnnotatedMethods(PNGDecoding.class);
        pngDecoding.defineAnnotatedMethods(PNGEncoding.class);
        pngDecoding.defineAnnotatedMethods(Color.class);
        pngDecoding.defineAnnotatedMethods(Operations.class);
    }
}
