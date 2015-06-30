#!/bin/sh

IP=$(ip addr | grep 'state UP' -A2 | tail -n1 | awk '/inet/{print substr($2,0)}' | cut -f1 -d'/')
echo $IP

echo "changing reader config"
vagrant ssh -c "sudo sed -i s/https/http/g /etc/datasift/gnip-reader/reader.json"
vagrant ssh -c "sudo sed -i s/stream.gnip.com/${IP}:5001/g /etc/datasift/gnip-reader/reader.json"

echo "changing writer config"
vagrant ssh -c "sudo sed -i s/https/http/g /etc/datasift/datasift-writer/writer.json"
vagrant ssh -c "sudo sed -i s/in.datasift.com/${IP}/g /etc/datasift/datasift-writer/writer.json"
vagrant ssh -c "sudo sed -i s/443/5002/g /etc/datasift/datasift-writer/writer.json"

echo "restart the services"
vagrant ssh -c "sudo supervisorctl restart gnip-reader"
vagrant ssh -c "sudo supervisorctl restart datasift-writer"
