#!/bin/bash
RUBY=/opt/homebrew/opt/ruby@3.2/bin
cd "$(dirname "$0")"
$RUBY/gem install bundler --no-document -q
$RUBY/bundle install --quiet
$RUBY/bundle exec jekyll serve
