require 'asciidoctor'
require 'erb'
require './src/main/ruby/readme.rb'

options = {:mkdirs => true, :safe => :unsafe, :attributes => 'linkcss'}

guard 'shell' do
  watch(/^src\/[A-Za-z].*\.adoc$/) {|m|
    SpringCloud::Build.render_file('src/main/asciidoc/README.adoc', :to_file => './README.adoc')
    Asciidoctor.render_file('src/main/asciidoc/spring-cloud-netflix.adoc', options.merge(:to_dir => 'target/generated-docs'))
  }
end
