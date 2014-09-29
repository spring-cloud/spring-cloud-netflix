#!/usr/bin/env ruby

base_dir = File.join(File.dirname(__FILE__),'../../..')
src_dir = File.join(base_dir, "/src/main/asciidoc")
require File.join(File.dirname(__FILE__), 'readme.rb')

options = {}
ARGV.length > 0 and options[:to_file] = ARGV[0]

SpringCloud::Build.render_file("#{src_dir}/README.adoc", options)
