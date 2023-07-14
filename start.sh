#!/bin/bash

service mysql restart
service redis-server restart

cd /root/ZLMediaKit/release/linux/Debug 
nohup ./MediaServer  > log1 2 1>&1 &
cd /root/wvp-GB28181-pro/target
nohup java -jar wvp-pro*.jar > log2 2 1>&1 &

