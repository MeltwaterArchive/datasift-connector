require 'spec_helper'

describe 'datasift-stats::default' do

  describe package('influxdb') do
    it { should be_installed }
  end

  describe user('influxdb') do
    it { should exist }
  end

  describe service('influxdb') do
    it { should be_running.under('supervisor') }
  end

  [2003, 8083, 8086, 8090].each do |port|
    describe port(port) do
      it { should be_listening }
    end
  end

  describe user('statsd') do
    it { should exist }
  end

  describe service('statsd') do
    it { should be_running.under('supervisor') }
  end

  describe port(8125) do
    it { should be_listening }
  end

  describe user('grafana') do
    it { should exist }
  end

  describe service('grafana') do
    it { should be_running.under('supervisor') }
  end

  describe port(3000) do
    it { should be_listening }
  end

  describe file('/var/lib/grafana/grafana.db') do
    it { should be_file }
    its(:md5sum) { should eq '970175db812f0014c22e6762045bb186' }
    it { should be_owned_by 'grafana' }
    it { should be_grouped_into 'grafana' }
    it { should be_readable }
    it { should be_writable.by('owner') }
  end

end

describe 'datasift-kafka::default' do

  describe package('tar') do
    it { should be_installed }
  end

  describe user('kafka') do
    it { should exist }
  end

  describe service('zookeeper') do
    it { should be_running.under('supervisor') }
  end

  describe service('kafka') do
    it { should be_running.under('supervisor') }
  end

  describe file('/opt/kafka/libs/kafka-statsd-metrics2-0.3.0.jar') do
    it { should be_file }
    it { should be_owned_by 'kafka' }
    it { should be_grouped_into 'kafka' }
    it { should be_readable }
  end

  describe file('/opt/kafka/config/server.properties') do
    its(:content) do
      should match( \
        /kafka.metrics.reporters=com.airbnb.kafka.KafkaStatsdMetricsReporter/)
    end
    its(:content) do
      should match(/external.kafka.statsd.reporter.enabled=true/)
    end
    its(:content) do
      should match(/external.kafka.statsd.tag.enabled=false/)
    end
    its(:content) do
      should match(/external.kafka.statsd.metrics.prefix=connector/)
    end
  end
end

describe 'datasift-writer::default' do

  describe package('datasift-writer') do
    it { should be_installed }
  end

  describe user('writer') do
    it { should exist }
  end

  describe file('/etc/datasift/datasift-writer/writer.json') do
    it { should be_owned_by 'writer' }
    # TODO: find a way to check the md5sum after attribute injection
    # its(:md5sum) { should eq '71894fc93e907e0ba1171f3497d1baa9' }
  end

  describe service('datasift-writer') do
    it { should be_running.under('supervisor') }
  end

  describe command('yum info datasift-writer | '\
                   "grep \"Repo        : installed\"") do
    its(:exit_status) { should eq 0 }
  end

end

describe 'gnip-reader::default' do

  describe package('gnip-reader') do
    it { should be_installed }
  end

  describe user('reader') do
    it { should exist }
  end

  describe file('/etc/datasift/gnip-reader/reader.json') do
    it { should be_owned_by 'reader' }
    # TODO: find a way to check the md5sum after attribute injection
    # its(:md5sum) { should eq 'c30857f2dab6edd3b147ad45a6a9b563' }
  end

  describe service('gnip-reader') do
    it { should be_running.under('supervisor') }
  end

  describe command('yum info gnip-reader | '\
                   "grep \"Repo        : installed\"") do
    its(:exit_status) { should eq 0 }
  end

end

describe 'twitterapi-reader::default' do

  describe package('twitterapi-reader') do
    it { should be_installed }
  end

  describe user('reader') do
    it { should exist }
  end

  describe file('/etc/datasift/twitterapi-reader/reader.json') do
    it { should be_owned_by 'reader' }
    # TODO: find a way to check the md5sum after attribute injection
  end

  # This service is stopped by default. So no running test.

  describe command('yum info twitterapi-reader | '\
                   "grep \"Repo        : installed\"") do
    its(:exit_status) { should eq 0 }
  end

end

=begin
describe 'historics-reader::default' do

  describe package('historics-reader') do
    it { should be_installed }
  end

  describe user('historicsreader') do
    it { should exist }
  end

  describe file('/etc/datasift/historics-reader/reader.json') do
    it { should be_owned_by 'historicsreader' }
    its(:md5sum) { should eq 'cee5e0e34249e0ee617daf3b734fc4b1' }
  end

end
=end

describe 'historics-api::default' do

  describe package('nodejs') do
    it { should be_installed }
  end

  describe user('historicsapi') do
    it { should exist }
  end

  describe file('/usr/lib/datasift/historics-api/config.js') do
    it { should be_owned_by 'historicsapi' }
    its(:md5sum) { should eq '3a5e37ad317a5ad0ddc3abc2f9ab3f6b' }
  end

  describe service('historics-api') do
    it { should be_running.under('supervisor') }
  end

  describe file('/usr/lib/datasift/historics-api/server.js') do
    it { should exist }
  end

  describe file('/usr/lib/datasift/historics-api/lib/jobs.js') do
    it { should exist }
  end

  describe file('/usr/lib/datasift/historics-api/lib/utils.js') do
    it { should exist }
  end

  describe file('/usr/lib/datasift/historics-api/controllers/historics.js') do
    it { should exist }
  end

  describe file('/usr/lib/datasift/historics-api/db.sqlite') do
    it { should exist }
  end

end

describe 'webapp::default' do

  describe package('nodejs') do
    it { should be_installed }
  end

  describe file('/etc/nginx/nginx_datasift.conf') do
    it { should be_file }
    it { should be_mode 644 }
    it { should be_owned_by 'root' }
    it { should be_grouped_into 'root' }
    its(:content) { should match(/daemon off;/) }
  end

  describe service('nginx') do
    it { should be_running.under('supervisor') }
  end

end
