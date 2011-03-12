Dir['tasks/*.rake'].each { |file| load(file) }

gem_management_tasks = GithubGem::RakeTasks.new(:gem)

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
    ant.javac :srcdir => 'ext/oily_png/java', :destdir => 'tmp/java'
    ant.jar :basedir => 'tmp/java', :destfile => 'ext/oily_png.jar'
  end
else
  require 'rake/extensiontask'
  Rake::ExtensionTask.new('oily_png', gem_management_tasks.gemspec) do |ext|
    ext.lib_dir = File.join('lib', 'oily_png')
  end
end

Rake::Task['spec:basic'].prerequisites << :compile
Rake::Task['spec:rcov'].prerequisites << :compile
Rake::Task['spec:specdoc'].prerequisites << :compile

task :default => [:spec]
