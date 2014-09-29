require 'asciidoctor'
require 'erb'

options = {:mkdirs => true, :safe => :unsafe, :attributes => 'linkcss'}

guard 'shell' do
  watch(/^[A-Za-z].*\.adoc$/) {|m|
    Asciidoctor.render_file('src/main/adoc/README.adoc', options.merge(:to_file => './README.md'))
    Asciidoctor.render_file('src/main/adoc/spring-cloud-netflix.adoc', options.merge(:to_dir => 'target/docs'))
  }
end
