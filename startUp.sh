#!/usr/bin/env bash

BROADCAST_IP=172.17.255.255
#IP_ADDR=`ifconfig | grep -i 172.17 | sed 's/:/ /g' | awk '{print $3}'`
IP_ADDR=`ip addr | grep -i 172.17 | sed 's/:/ /g' | sed 's/\// /' | awk '{print $2}'`

java -Dapp.ipRange=$IP_ADDR -Dapp.broadcastAddr=$BROADCAST_IP -jar /app.jar
