
include_recipe 'nodejs'
include_recipe 'git'

user 'statsd'

git node['statsd']['path'] do
  repository node['statsd']['repo']
  revision node['statsd']['version']
  action :sync
end

execute 'install dependencies' do
  command 'npm install -d'
  cwd node['statsd']['path']
end

directory node['statsd']['config_dir'] do
  owner node['statsd']['user']
  group node['statsd']['group']
  mode 0755
end

template "#{node['statsd']['config_dir']}/config.js" do
  source 'config.js.erb'
  owner node['statsd']['user']
  group node['statsd']['group']
  mode 0644

  config_hash = {
    graphitePort: node['statsd']['graphite_port'],
    graphiteHost: node['statsd']['graphite_host'],
    flushInterval: node['statsd']['flush_interval_msecs'],
    port: node['statsd']['port'],
    deleteIdleStats: node['statsd']['delete_idle_stats'],
    backends: ['./backends/graphite', './backends/console']
  }

  if node['statsd']['graphite_enabled']
    config_hash['graphite'] = { 'legacyNamespace' => true }
    config_hash['graphitePort'] = node['statsd']['graphite_port']
    config_hash['graphiteHost'] = node['statsd']['graphite_host']
  end

  variables config_hash: config_hash
end

supervisor_service 'statsd' do
  user node['statsd']['user']
  directory node['statsd']['path']
  command "node stats.js #{node['statsd']['config_dir']}/config.js"
end
