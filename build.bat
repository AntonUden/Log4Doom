@echo off
echo Compiling payload
javac -cp "./lib/jna-platform-5.10.0.jar;./lib/jna-5.10.0.jar" Log4DoomPayload.java
echo Building container
docker build -t zeeraa/log4doom .
echo Done
pause