#
# Cookbook Name:: datasift-kafka
# Recipe:: default
#
# Copyright (c) 2015 The Authors, All Rights Reserved.

package 'tar'

user 'kafka'

include_recipe 'kafka::_defaults'
include_recipe 'kafka::_setup'
include_recipe 'kafka::_install'

directory node.kafka.config_dir do
  owner node.kafka.user
  group node.kafka.group
  mode '755'
  recursive true
end

template ::File.join(node.kafka.config_dir, 'log4j.properties') do
  source 'log4j.properties.erb'
  owner node.kafka.user
  group node.kafka.group
  mode '644'
  helpers(Kafka::Log4J)
  variables(
    config: node.kafka.log4j
  )
end

template ::File.join(node.kafka.config_dir, 'server.properties') do
  source 'server.properties.erb'
  owner node.kafka.user
  group node.kafka.group
  mode '644'
  helper :config do
    node.kafka.broker.sort_by(&:first)
  end
  helpers(Kafka::Configuration)
end

supervisor_service 'zookeeper' do
  user 'root'
  command "#{node.kafka.install_dir}/bin/zookeeper-server-start.sh "\
          '/opt/kafka/config/zookeeper.properties'
end

service 'kafka' do
  action :disable
  action :stop
end

supervisor_service 'kafka' do
  user 'kafka'
  command "/bin/bash -c '"\
          'sleep 20 && '\
          "#{node.kafka.install_dir}/bin/kafka-run-class.sh "\
          "kafka.Kafka #{node.kafka.config_dir}/server.properties"\
          "'"
end

remote_file \
  "#{node.kafka.statsd_install_dir}/kafka-statsd-metrics2-0.3.0.jar" do
  source node.kafka.statsd_remote_url
  owner node.kafka.user
  group node.kafka.group
  mode '644'
  action :create
  notifies :restart, 'supervisor_service[kafka]', :delayed
end

execute 'stop_initial_kafka' do
  user 'root'
  command 'ps ax | grep -i \'kafka\.Kafka\' | grep java | grep -v grep |'\
    ' awk \'{print $1}\' | xargs kill -s SIGINT'
  returns [123, 0]
end
