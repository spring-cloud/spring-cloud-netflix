require 'asciidoctor'
require 'erb'

options = {:mkdirs => true, :safe => :unsafe, :attributes => ['linkcss', 'allow-uri-read']}

guard 'shell' do
  watch(/^docs\/[A-Z-a-z][^#]*\.adoc$/) {|m|
    Asciidoctor.load_file('docs/src/main/asciidoc/README.adoc', :to_file => './README.adoc', safe: :safe, parse: false, attributes: 'allow-uri-read')
    Asciidoctor.render_file('docs/src/main/asciidoc/spring-cloud-netflix.adoc', options.merge(:to_dir => 'target/generated-docs'))
  }
end
