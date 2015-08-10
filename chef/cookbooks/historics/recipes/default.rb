#
# Cookbook Name:: historics
# Recipe:: default
#
# Copyright (c) 2015 The Authors, All Rights Reserved.

local_dir = node['historics']['local_dir']

user 'historics'

directory '/usr/lib/datasift/historics' do
  owner 'historics'
  mode '0755'
  action :create
end

template '/usr/lib/datasift/historics/config.json' do
  owner 'historics'
  action :create_if_missing
end

supervisor_service 'historics-api' do
  user 'historics'
  command 'node /usr/lib/datasift/historics/server.js'
  directory '/usr/lib/datasift/historics'
  autostart true
end
