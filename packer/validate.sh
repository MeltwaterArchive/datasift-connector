#!/bin/bash
set -ex

cd ../chef
berks vendor vendor/cookbooks

cd ../packer

for file in *.json
do
  packer validate ${file}
done
