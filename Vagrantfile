# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "opscode_centos-6.5_chef-provisionerless"
  config.vm.box_url = "http://opscode-vm-bento.s3.amazonaws.com/vagrant/virtualbox/opscode_centos-6.5_chef-provisionerless.box"

  config.vm.network "public_network"
  config.vm.network "forwarded_port", guest: 80, host: 8080
  config.vm.network "forwarded_port", guest: 443, host: 8443
  config.vm.network "forwarded_port", guest: 3000, host: 3000
  config.vm.network "forwarded_port", guest: 8083, host: 8083
  config.vm.network "forwarded_port", guest: 8086, host: 8086
  config.vm.network "forwarded_port", guest: 8888, host: 8888

  config.omnibus.chef_version = '12.17.44'

  config.berkshelf.berksfile_path = "chef/Berksfile"
  config.berkshelf.enabled = true

  VAGRANT_JSON = JSON.parse(Pathname(__FILE__).dirname.join('chef', 'nodes', 'datasift-connector.json').read)

  config.vm.provision :chef_zero do |chef|
    #chef.cookbooks_path = ["chef/cookbooks"]
    chef.roles_path = "chef/roles"
    chef.nodes_path = "chef/nodes"
    chef.data_bags_path = "chef/data_bags"
    chef.provisioning_path = "/tmp/vagrant-chef"

    chef.run_list = VAGRANT_JSON.delete('run_list')
    chef.json = VAGRANT_JSON

    chef.log_level = :info
  end
end
