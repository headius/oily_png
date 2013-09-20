require "bundler/gem_tasks"
require "rspec/core/rake_task"
require "rake/extensiontask"

if defined?(RUBY_ENGINE) && RUBY_ENGINE == "jruby"
  # Could not get this working
  # require 'rake/javaextensiontask'
  # Rake::JavaExtensionTask.new('oily_png', gem_management_tasks.gemspec) do |ext|
  #   ext.lib_dir = File.join('lib', 'oily_png')
  #   ext.ext_dir = File.join('ext', 'java')
  #   ext.source_pattern = "**/*.java"
  # end
  require 'ant'
  
  task :compile do
    mkdir_p 'tmp/java'
    ant.javac :srcdir => 'ext/oily_png/java', :destdir => 'tmp/java', :source => '1.6', :target => '1.6'
    ant.jar :basedir => 'tmp/java', :destfile => 'lib/oily_png/oily_png.jar'
  end
else
  require 'rake/extensiontask'

  Rake::ExtensionTask.new('oily_png') do |ext|
    ext.lib_dir = File.join('lib', 'oily_png')
    ext.config_options = '--with-cflags="-std=c99"'
  end
end

Dir['tasks/*.rake'].each { |file| load(file) }
RSpec::Core::RakeTask.new(:spec) do |task|
  task.pattern = "./spec/**/*_spec.rb"
  task.rspec_opts = ['--color']
end

Rake::Task['spec'].prerequisites << :compile

task :default => [:spec]
