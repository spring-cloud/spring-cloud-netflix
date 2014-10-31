require 'asciidoctor'
require 'erb'

options = {:mkdirs => true, :safe => :unsafe, :attributes => 'linkcss'}

guard 'shell' do
  watch(/^src\/[A-Za-z].*\.adoc$/) {|m|
    Asciidoctor.load_file('src/main/asciidoc/README.adoc', :to_file => './README.adoc', safe: :safe, parse: false, attributes: 'allow-uri-read')
    Asciidoctor.render_file('src/main/asciidoc/spring-cloud-netflix.adoc', options.merge(:to_dir => 'target/generated-docs'))
  }
end
