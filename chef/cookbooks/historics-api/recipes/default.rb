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

directory '/usr/lib/datasift/historics-api/controllers' do
  owner 'historicsapi'
  mode '0755'
  action :create
end

directory '/usr/lib/datasift/historics-api/lib' do
  owner 'historicsapi'
  mode '0755'
  action :create
end

directory '/var/log/datasift' do
  owner 'historics-reader'
  mode '0777'
  action :create
end

template '/usr/lib/datasift/historics-api/config.json' do
  owner 'historicsapi'
  action :create_if_missing
end

cookbook_file 'historics.js' do
  path '/usr/lib/datasift/historics-api/controllers/historics.js'
  owner 'historics-api'
  group 'historics-api'
end

cookbook_file 'jobs.js' do
  path '/usr/lib/datasift/historics-api/lib/jobs.js'
  owner 'historics-api'
  group 'historics-api'
end

cookbook_file 'utils.js' do
  path '/usr/lib/datasift/historics-api/lib/utils.js'
  owner 'historics-api'
  group 'historics-api'
end

cookbook_file 'server.js' do
  path '/usr/lib/datasift/historics-api/server.js'
  owner 'historics-api'
  group 'historics-api'
end

cookbook_file 'db.sqlite' do
  path '/usr/lib/datasift/historics-api/db.sqlite'
  owner 'historics-api'
  group 'historics-api'
end

cookbook_file 'package.json' do
  path '/usr/lib/datasift/historics-api/package.json'
  owner 'historics-api'
  group 'historics-api'
end

execute "npm-install-app" do
  cwd '/usr/lib/datasift/historics-api'
  command 'npm install > npm-run.log 2>&1'
  user node['nodejs']['historicsapi']
  action :run
end

supervisor_service 'historics-api' do
  user 'historicsapi'
  command 'node /usr/lib/datasift/historics/server.js'
  directory '/usr/lib/datasift/historics'
  autostart true
end