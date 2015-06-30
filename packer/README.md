# Packer image generation for Datasift Connector

See [Packer](https://www.packer.io/docs) for more details on using Packer. These configuration files have been tested with Packer 0.8.


## Building

To create an AMI and docker image of the latest release use:

`$ ./build.sh [AWS access key] [AWS secret key]`

This will create an AMI on AWS, and therefore *you will be charged*.

To build images using local versions of the components rather than the latest release on Github (useful for testing) use:

`$ ./build.sh [AWS access key] [AWS secret key] ami-local-rpms.json`

ensuring that the rpms are in the `./packer/` directory and are named `gnip-reader-latest.noarch.rpm` and `datasift-writer-latest.noarch.rpm`.

It is also possible to build docker images using the scripts `docker.json` and `docker-local-rpms.json`. In the future it would be nice to resolve the differences in these and the ami files, create a single packer file to build both and allow packer to build them in parallel.

## Output

An AMI `datasift-connector {date}` will be created in EC2 for the account whose credentials were passed in. An instance can be launched from the AWS EC2 console by selecting the AMI and clicking launch.

A Docker image `datasift_connector_docker_image.tar` will be created in `./packer/` this can be imported as a Docker image using something like `cat datasift_connector_docker_image.tar | docker import - packerimagelocal:new`.

## Notes

Authorized keys must removed at the end of the build to allow the user to SSH into the machine once launched. See [here](https://github.com/mitchellh/packer/issues/185#issuecomment-20997766) for details.
