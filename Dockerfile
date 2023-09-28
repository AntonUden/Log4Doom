FROM ubuntu:latest

WORKDIR /log4doom

ENV PUBLIC_IP="127.0.0.1"

# === Install packages ===
RUN apt-get update

# Java and Maven
RUN apt-get install -y openjdk-8-jdk
RUN apt-get install -y maven

# NodeJS
RUN apt-get -y install curl gnupg
RUN curl -sL https://deb.nodesource.com/setup_20.x  | bash -
RUN apt-get -y install nodejs

# === Copy files and setting up directories ===

# Marshalsec
COPY marshalsec-0.0.3-SNAPSHOT-all.jar marshalsec-0.0.3-SNAPSHOT-all.jar

# Payload web server
COPY PayloadServer /log4doom/PayloadServer
RUN mkdir /log4doom/PayloadServer/payload
WORKDIR /log4doom/PayloadServer
RUN npm install
COPY Log4DoomPayload.java /log4doom/PayloadServer/payload/Log4DoomPayload.java

# Minecraft server
COPY MinecraftServer /log4doom/MinecraftServer
RUN mkdir /log4doom/MinecraftServer/plugins

# Compiling the exploit
WORKDIR /log4doom/PayloadServer/payload
RUN javac Log4DoomPayload.java
WORKDIR /log4doom

# Compile plugins
RUN mkdir /build
COPY Log4DoomPlugin /build/Log4DoomPlugin
WORKDIR /build/Log4DoomPlugin
RUN mvn clean package

# Install plugins
RUN cp /build/Log4DoomPlugin/target/Log4Doom-1.0.0-SNAPSHOT.jar /log4doom/MinecraftServer/plugins/Log4Doom.jar
WORKDIR /log4doom/MinecraftServer/plugins
COPY ./plugins/*.jar .
WORKDIR /log4doom

# Main script
COPY start.sh .

# Expose the ports
EXPOSE 1389
EXPOSE 3000
EXPOSE 25565

CMD ["bash", "./start.sh"]