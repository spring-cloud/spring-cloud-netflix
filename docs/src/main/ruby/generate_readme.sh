#!/usr/bin/env ruby

base_dir = File.join(File.dirname(__FILE__),'../../..')
src_dir = File.join(base_dir, "/src/main/asciidoc")
require File.join(File.dirname(__FILE__), 'readme.rb')
require 'optparse'

options = {}
input = "#{src_dir}/README.adoc"

OptionParser.new do |o|
  o.on('-o OUTPUT_FILE', 'Output file (default is stdout)') { |file| options[:to_file] = file unless file=='-' }
  o.on('-h', '--help') { puts o; exit }
  o.parse!
end

input = ARGV[0] if ARGV.length>0

SpringCloud::Build.render_file(input, options)
