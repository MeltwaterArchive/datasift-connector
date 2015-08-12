#
# Cookbook Name:: historics-reader
# Recipe:: default
#
# Copyright (c) 2015 The Authors, All Rights Reserved.

chef_gem 'octokit' do
  action :install
end

require 'octokit'

local_dir = node['writer']['local_dir']

# Allow install from a local file, useful for testing
execute "find #{local_dir} -name 'historics-reader*.rpm' " \
        '| sort | tail -n1 |' \
        " xargs -I '{}' cp {} #{Chef::Config[:file_cache_path]}" \
        '/datasift-writer-latest.rpm' do
  only_if { Dir.glob("#{local_dir}historics-reader*.rpm").any? }
end

# Usually want to get from Github releases
remote_file "#{Chef::Config[:file_cache_path]}/datasift-writer-latest.rpm" do
  source lazy {
    release = Octokit.latest_release(node['writer']['repo'])
    asset = release.assets.select { |r| r.name.match(/^datasift-writer/) }
    asset[0].browser_download_url
  }
  action :create
  not_if { Dir.glob("#{local_dir}historics-reader*.rpm").any? }
end

rpm_package 'datasift-writer' do
  source "#{Chef::Config[:file_cache_path]}/historics-reader-latest.rpm"
  action :install
end

user 'historicsreader'

directory '/etc/datasift/historics-reader' do
  owner 'historicsreader'
  mode '0755'
  action :create
end

template '/etc/datasift/historics-reader/reader.json' do
  owner 'writer'
  action :create_if_missing
end

cron_d 'historics-reader-cron' do
  minute  '*/5'
  command 'java -cp "/etc/datasift/*:/etc/datasift/:'\
          '/etc/datasift/historics-reader/:'\
          '/usr/lib/datasift/historics-reader/:'\
          '/usr/lib/datasift/historics-reader/*" '\
          'com.datasift.connector.HistoricsReader '\
          '/etc/datasift/historics-reader/reader.json'
  user    'historicsreader'
end
