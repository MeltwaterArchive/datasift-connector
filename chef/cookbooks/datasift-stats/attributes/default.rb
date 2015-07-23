
default['statsd']['repo'] = 'https://github.com/etsy/statsd.git'
default['statsd']['version'] = 'v0.7.2'
default['statsd']['config_dir'] = '/etc/statsd'
default['statsd']['path'] = '/usr/share/statsd'
default['statsd']['user'] = 'statsd'
default['statsd']['group'] = 'statsd'
default['statsd']['flush_interval_msecs'] = 10_000
default['statsd']['port'] = 8125
default['statsd']['delete_idle_stats'] = false
default['statsd']['graphite_port'] = 2003
default['statsd']['graphite_host'] = 'localhost'

default['influxdb']['source'] = \
  'https://s3.amazonaws.com/influxdb/influxdb-0.8.8-1.x86_64.rpm'

default['influxdb']['version'] = '0.8'

# Parameters to configure InfluxDB
# Based on https://github.com/influxdb/influxdb/blob/v0.8.5/config.sample.toml
default['influxdb']['config'] = {
  'bind-address' => '0.0.0.0',
  'reporting-disabled' => false,
  logging: {
    level: 'info',
    file: '/opt/influxdb/shared/log.txt'
  },
  admin: {
    port: 8083
  },
  api: {
    port: 8086,
    'read-timeout' => '5s'
  },
  input_plugins: {
    graphite: {
      enabled: true,
      port: 2003,
      udp_enabled: true,
      database: 'datasift'
    },
    udp: {
      enabled: false
    }
  },
  raft: {
    port: 8090,
    dir: '/opt/influxdb/shared/data/raft'
  },
  storage: {
    dir: '/opt/influxdb/shared/data/db',
    'write-buffer-size' => 10_000,
    'default-engine' => 'rocksdb',
    'max-open-shards' => 0,
    'point-batch-size' => 100,
    'write-batch-size' => 5_000_000,
    'retention-sweep-period' => '10m',
    engines: {
      leveldb: {
        'max-open-files' => 1000,
        'lru-cache-size' => '200m'
      },
      rocksdb: {
        'max-open-files' => 1000,
        'lru-cache-size' => '200m'
      },
      hyperleveldb: {
        'max-open-files' => 1000,
        'lru-cache-size' => '200m'
      },
      lmdb: {
        'map-size' => '100g'
      }
    }
  },
  cluster: {
    'protobuf_port' => 8099,
    'protobuf_timeout' => '2s',
    'protobuf_heartbeat' => '200ms',
    'protobuf_min_backoff' => '1s',
    'protobuf_max_backoff' => '10s',
    'write-buffer-size' => 1_000,
    'max-response-buffer-size' => 100,
    'concurrent-shard-query-limit' => 10
  },
  wal: {
    dir: '/opt/influxdb/shared/data/wal',
    'flush-after' => 1_000,
    'bookmark-after' => 1_000,
    'index-after' => 1_000,
    'requests-per-logfile' => 10_000
  }
}
