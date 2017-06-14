
include_recipe 'nodejs'
include_recipe 'nginx'

service 'nginx' do
  action :disable
end

service 'nginx' do
  action :stop
end

template '/etc/nginx/nginx_datasift.conf' do
  user 'root'
  group 'root'
  mode 0o644
  source 'nginx.conf.erb'
end

supervisor_service 'nginx' do
  user 'root'
  command '/usr/sbin/nginx -c /etc/nginx/nginx_datasift.conf'
end
