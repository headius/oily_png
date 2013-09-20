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
        RubyModule resampling = oilyPng.defineModuleUnder("Resampling");

        pngDecoding.defineAnnotatedMethods(PNGDecoding.class);
        pngEncoding.defineAnnotatedMethods(PNGEncoding.class);
        color.defineAnnotatedMethods(Color.class);
        operations.defineAnnotatedMethods(Operations.class);
        operations.defineAnnotatedMethods(Resampling.class);
    }
}
