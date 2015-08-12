#
# Cookbook Name:: historics-api
# Recipe:: default
#
# Copyright (c) 2015 The Authors, All Rights Reserved.

user 'historicsapi'

directory '/usr/lib/datasift/historics-api' do
  owner 'historicsapi'
  mode '0755'
  action :create
end

template '/usr/lib/datasift/historics-api/config.json' do
  owner 'historicsapi'
  action :create_if_missing
end



supervisor_service 'historics-api' do
  user 'historicsapi'
  command 'node /usr/lib/datasift/historics/server.js'
  directory '/usr/lib/datasift/historics'
  autostart true
end
