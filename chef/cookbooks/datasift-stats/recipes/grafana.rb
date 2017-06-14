
yum_package 'fontconfig' do
  action :install
end

remote_file "#{Chef::Config[:file_cache_path]}/grafana_latest.rpm" do
  source 'https://grafanarel.s3.amazonaws.com/builds/grafana-2.0.2-1.x86_64.rpm'
  action :create
end

rpm_package 'grafana' do
  source "#{Chef::Config[:file_cache_path]}/grafana_latest.rpm"
  action :install
end

template '/etc/grafana/grafana-datasift.ini' do
  source 'grafana.ini.erb'
  owner 'grafana'
  group 'grafana'
  mode '0644'
end

env = { 'HOME' => '/usr/share/grafana' }
supervisor_service 'grafana' do
  user 'grafana'
  command '/usr/sbin/grafana-server '\
          '--pidfile=/var/tmp/grafana-server.pid '\
          '--config=/etc/grafana/grafana.ini '\
          'cfg:default.paths.data=/var/lib/grafana '\
          'cfg:default.paths.logs=/var/log/grafana'
  environment env
  directory '/usr/share/grafana'
end

cookbook_file 'grafana.db' do
  path '/var/lib/grafana/grafana.db'
  owner 'grafana'
  group 'grafana'
end
