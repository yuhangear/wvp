service mysql restart
service redis-server restart

cd /root/ZLMediaKit/release/linux/Debug 
nohup ./MediaServer -d  &
cd /root/wvp-GB28181-pro/target
nohup java -jar wvp-pro-2.6.9-071316*.jar &