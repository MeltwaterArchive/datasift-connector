# DataSift Connector

The DataSift Connector is a set of components that enable you to retrieve data from third-party APIs and storage systems, and to pass that data into the DataSift platform for processing.

*Note that the current implementation only has built-in support for Gnip as a data source, but more sources will be coming soon. Note that you cannot upload arbitrary data - the platform requires support for the data format to be present. If you require further details on the potential for sending other data to DataSift please contact your account executive.*

## Before You Start

To deploy the connector or to work with it locally, you'll first need to clone the repo:

- `git clone https://github.com/datasift/datasift-connector.git`
- `cd datasift-connector`
- `git checkout tags/x.y.z-1` where x.y.z is the tag of the [release](https://github.com/datasift/datasift-connector/releases) required.


## Quick Start - Deployment to EC2

To run an instance of the connector on EC2:

- Ensure [Packer](https://www.packer.io/docs/installation.html) is installed.
- `cd packer`
- `./build.sh [AWS_ACCESS_KEY] [AWS_SECRET_KEY]`
- Once the Packer build has finished log on to your AWS dashboard, select the EC2 service and then click `AMIs`.
- Launch an instance of the built AMI using the standard EC2 mechanism.

After launching an instance, you'll next need to configure it:

- SSH into the instance. `ssh -i [PEM] root@[EC2-INSTANCE]`
- `sudo vi /etc/datasift/gnip-reader/reader.json` and add your Gnip credentials.
- `sudo vi /etc/datasift/datasift-writer/writer.json` and add your DataSift credentials.
- `sudo supervisorctl restart gnip-reader`
- `sudo supervisorctl restart datasift-writer`
- `exit`
- Open your browser to port 3000 of the launched EC2 machine. User `admin`, password `admin`. Click on the dashboard to see relevant metrics.

You will now be ingesting your Gnip data into DataSift.

## Quick Start - Local Development

To run a local instance of the connector do the following:

- Ensure [Vagrant](#vagrant) and relevant plug-ins are installed.
- Ensure a stable version of [VirtualBox](https://www.virtualbox.org) is installed.
- `vagrant up`

After launching an instance, you'll next need to configure it:

- SSH into the instance. eg. `vagrant ssh`
- `sudo vi /etc/datasift/gnip-reader/reader.json` and add your Gnip credentials.
- `sudo vi /etc/datasift/datasift-writer/writer.json` and add your DataSift credentials.
- `sudo supervisorctl restart gnip-reader`
- `sudo supervisorctl restart datasift-writer`
- `exit`
- Browse to `http://localhost:3000`. User `admin`, password `admin`. Click on the dashboard to see relevant metrics.

You will now be ingesting your Gnip data into DataSift.

## Installation

We recommend that you deploy the connector using our [Packer configuration](https://github.com/datasift/datasift-connector/tree/master/packer). We supply configuration examples for building an EC2 AMI and also a Docker image. For local testing you can use Vagrant to create a local virtual machine. The Vagrantfile we supply will create a VirtualBox image, but this is easily modified for other targets.

### Chef

We've used [Chef](https://www.chef.io/chef/) to ensure a repeatable build for development, testing and deployment. This uses [Berkshelf](http://berkshelf.com/) to manage dependencies and [Test Kitchen](http://kitchen.ci/) to perform integration tests, both are  from the [Chef Development Kit](https://downloads.chef.io/chef-dk/).

### Vagrant

We use [Vagrant](https://www.vagrantup.com/) for development and testing, and it can also be used for deployment. We recommend using the latest version, 1.7.0+ is required. We use a few plugins to make life easier:

* [vagrant-omnibus](https://github.com/chef/vagrant-omnibus)
* [vagrant-berkshelf](https://github.com/berkshelf/vagrant-berkshelf) (also make sure you've installed [ChefDK](https://downloads.chef.io/chef-dk/) as mentioned above)

To get started run `vagrant up` from the root directory. If chef fails the first time try `berks update`, `vagrant destroy` then `vagrant up`.

## Connector Design

To give some context, the first diagram below shows where this connector fits into the DataSift Data Ingestion API. The second diagram shows the design of this connector.

### Data Ingestion API

![DataSift Data Ingestion API Overview](https://raw.githubusercontent.com/datasift/datasift-connector/master/docs/overview.png)

### Connector

![DataSift Connector Design](https://raw.githubusercontent.com/datasift/datasift-connector/master/docs/design.png)

### Data Flow

The connector contains three main parts: a reader, a buffer, and a writer:

* The Gnip Reader will connect to the Gnip streaming API and pass the data received in to the buffer.
* The buffer is there to prevent data loss should there be an issue with the connection to the DataSift Data Ingestion API. One item in the queue is expected to be a single piece of data, i.e. a tweet, retweet, delete, etc.
* The DataSift Writer handles connecting to the DataSift Data Ingestion API and will send the data it pulls out of the buffer up to the DataSift platform.

### Metrics

All components log metrics to statsd so you can easily integrate it into your monitoring infrastructure. We also provide a default Grafana dashboard for monitoring the connector which can be found at `http://[connector-machine]:3000`, or `http://localhost:3000` if using the provided Vagrant file. The default login for Grafana is set to admin / admin.

## Configuration

### Gnip Reader

The Gnip Reader configuration file is located at `/etc/datasift/gnip-reader/reader.json` when deployed using the included Chef recipe.

Example:

```json
{
  "gnip": {
    "account": "ACCOUNT",
    "label": "LABEL",
    "product": "PRODUCT",
    "username": "USER",
    "password": "PASSWORD",
    "host": "https://stream.gnip.com",
    "retries": 10,
    "buffer_size": 10000,
    "buffer_timeout": 500
  },
  "kafka": {
    "topic": "twitter-gnip",
    "servers": "localhost:6667",
    "retry-backoff": 1000,
    "reconnect-backoff": 1000
  },
  "metrics": {
    "host": "localhost",
    "port": 8125,
    "prefix": "gnip.reader",
    "reporting-time": 1
  }
}
```

The important part is the `gnip` section. This is where you specify your Gnip API credentials that will enable the reader to connect to Gnip and receive data.

### DataSift Writer

The DataSift Writer configuration file is located at `/etc/datasift/datasift-writer/writer.json` when deployed using the included Chef recipe.

Example:

```json
{
  "zookeeper": {
    "socket": "localhost:2181"
  },
  "kafka": {
    "topic": "twitter-gnip",
    "broker": "localhost",
    "port": 9092
  },
  "datasift": {
    "base_url": "https://in.datasift.com/",
    "port": 443,
    "username": "username",
    "api_key": "apikey",
    "source_id": "sourceid",
    "bulk_size": 100000,
    "bulk_items": 1000,
    "bulk_interval": 1000
  },
  "metrics": {
    "host": "localhost",
    "port": 8125,
    "prefix": "datasift.writer",
    "reporting-time": 1
  }
}
```

## Contributing

We are always thrilled to receive pull requests, and do our best to process them as fast as possible. To create a pull request:

1. Fork on GitHub.
2. Create a feature branch.
3. Commit your changes **with tests**.
4. New feature? Send a pull request against the `develop` branch.
5. Bug fix? Send a pull request against the `master` branch.

Not sure if that typo is worth a pull request? Do it! We, and all other users, will appreciate it.

Found a bug but you're not a developer? Please [create an issue in GitHub](https://github.com/datasift/datasift-connector/issues) and we'll get right on it!

## License

The DataSift Connector is MIT-licensed. See the LICENSE file for more info.
