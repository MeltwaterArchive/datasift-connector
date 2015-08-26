user 'influxdb'

include_recipe 'influxdb::default'

supervisor_service 'influxdb' do
  user 'influxdb'
  command '/usr/bin/influxdb -config /opt/influxdb/shared/config.toml'
end

influxdb_database 'datasift' do
  action :create
end

influxdb_user 'datasift' do
  password 'datasift'
  databases ['datasift']
  action :create
end

influxdb_admin 'datasift_admin' do
  password 'datasift_admin'
  action :create
end
