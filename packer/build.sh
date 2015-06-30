#!/bin/bash
set -ex

cd ../chef
berks vendor vendor/cookbooks

cd ../packer
FILE=${3-ami.json}
echo "packer build $FILE"
packer build -var "aws_access_key=$1" -var "aws_secret_key=$2" $FILE
