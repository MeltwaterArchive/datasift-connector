#
# Cookbook Name:: twitterapi-reader
# Recipe:: default
#
# Copyright (c) 2015 The Authors, All Rights Reserved.

chef_gem 'octokit' do
  action :install
end

require 'octokit'

local_dir = node['reader']['local_dir']

# Allow install from a local file, useful for testing
execute "find #{local_dir} -name 'twitterapi-reader*.rpm' " \
        '| sort | tail -n1 |' \
        " xargs -I '{}' cp {} #{Chef::Config[:file_cache_path]}" \
        '/twitterapi-reader-latest.rpm' do
  only_if { Dir.glob("#{local_dir}twitterapi-reader*.rpm").any? }
end

# Usually want to get from Github releases
remote_file "#{Chef::Config[:file_cache_path]}/twitterapi-reader-latest.rpm" do
  source lazy {
    release = Octokit.latest_release(node['reader']['repo'])
    asset = release.assets.select { |r| r.name.match(/^twitterapi-reader/) }
    asset[0].browser_download_url
  }
  action :create
  not_if { Dir.glob("#{local_dir}twitterapi-reader*.rpm").any? }
end

rpm_package 'twitterapi-reader' do
  source "#{Chef::Config[:file_cache_path]}/twitterapi-reader-latest.rpm"
  action :install
end

user 'reader'

directory '/etc/datasift/' do
  owner 'reader'
  mode '0755'
  action :create
end

template '/etc/datasift/twitterapi-reader/reader.json' do
  owner 'reader'
  action :create_if_missing
end

supervisor_service 'twitterapi-reader' do
  user 'reader'
  command 'java -cp "/etc/datasift/*:/etc/datasift/:'\
          '/etc/datasift/twitterapi-reader/:'\
          '/usr/lib/datasift/twitterapi-reader/:'\
          '/usr/lib/datasift/twitterapi-reader/*" '\
          'com.datasift.connector.TwitterApiReader '\
          '/etc/datasift/twitterapi-reader/reader.json'
  autostart false
end
