#
# Cookbook Name:: datasift-writer
# Recipe:: default
#
# Copyright (c) 2015 The Authors, All Rights Reserved.

chef_gem 'octokit' do
  action :install
end

require 'octokit'

local_dir = node['writer']['local_dir']

# Allow install from a local file, useful for testing
execute "find #{local_dir} -name 'datasift-writer*.rpm' " \
        '| sort | tail -n1 |' \
        " xargs -I '{}' cp {} #{Chef::Config[:file_cache_path]}" \
        '/datasift-writer-latest.rpm' do
  only_if { Dir.glob("#{local_dir}datasift-writer*.rpm").any? }
end

# Usually want to get from Github releases
remote_file "#{Chef::Config[:file_cache_path]}/datasift-writer-latest.rpm" do
  source lazy {
    release = Octokit.latest_release(node['writer']['repo'])
    asset = release.assets.select { |r| r.name.match(/^datasift-writer/) }
    asset[0].browser_download_url
  }
  action :create
  not_if { Dir.glob("#{local_dir}datasift-writer*.rpm").any? }
end

rpm_package 'datasift-writer' do
  source "#{Chef::Config[:file_cache_path]}/datasift-writer-latest.rpm"
  action :install
end

user 'writer'

directory '/etc/datasift/' do
  owner 'writer'
  mode '0755'
  action :create
end

template '/etc/datasift/datasift-writer/writer.json' do
  owner 'writer'
  action :create_if_missing
end

supervisor_service 'datasift-writer' do
  user 'writer'
  command 'java -cp "/etc/datasift/*:/etc/datasift/:'\
          '/etc/datasift/datasift-writer/:'\
          '/usr/lib/datasift/datasift-writer/:'\
          '/usr/lib/datasift/datasift-writer/*" '\
          'com.datasift.connector.DataSiftWriter '\
          '/etc/datasift/datasift-writer/writer.json'
end
