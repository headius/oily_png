if defined?(RUBY_ENGINE) && RUBY_ENGINE == 'jruby'
  # Load the JRuby extension
  require 'jruby'
  require 'oily_png/oily_png.jar'

  org.jruby.ext.oily_png.OilyPNG.load(JRuby.runtime)
else
  # load the normal C ext
  require 'oily_png'
end