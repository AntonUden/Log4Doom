# Log4Doom
My attempt at injecting doom into minecraft players game once they join a server

Note that this wont work unless you modify your game to be vulnerable to the log4j exploit again

## Disclaimer
This is for educational purposes only (and running doom on people's pcs). I do not condone any malicious use of this project.
Using this for malicious purposes is almost impossible since the client needs heavy modifications for this to work.

## Building the server
To build the docker container simply run the build.bat file

## Running the server
The docker container can be started with the following command
`docker run -it -p 1389:1389 -p 3000:3000 -p 25565:25565 -e PUBLIC_IP="192.168.1.200" --rm zeeraa/log4doom`
Change PUBLIC_IP to the ip of the machine running the exploit server. you also need to portforward 1389, 3000 and 25565.

## Making a vulnerable client
* Download MultiMC
* Create profile and edit it
* In Settings add the following java argument: `-Dcom.sun.jndi.ldap.object.trustURLCodebase=true`
* Go to Version and edit the Minecraft entry
* Replace log4j-api and log4j-core with the following values
```json
{
    "downloads": {
        "artifact": {
            "sha1": "9141212b8507ab50a45525b545b39d224614528b",
            "size": 1745700,
            "url": "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.14.1/log4j-core-2.14.1.jar"
        }
    },
    "name": "org.apache.logging.log4j:log4j-core:2.14.1"
},
{
    "downloads": {
        "artifact": {
            "sha1": "320a2dfd18513a5f41b4e75729df684488cbd925",
            "size": 55977,
            "url": "https://libraries.minecraft.net/tv/twitch/twitch/6.5/twitch-6.5.jar"
        }
    },
    "name": "tv.twitch:twitch:6.5"
}
```
* Fully remove the library called `com.mojang:netty`
## Extra info
##### Why does the minecraft window disapear and why is the icon missing?
Turns out the minecraft windows is not a JFrame so to get it working i had to use some user32 system calls to find the existing minecraft window, create an identical jframe and then hide the minecraft window.

##### Why remove the mojang netty library?
At first i was struggling to get the exploit working but it turns out that mojang added a class in their version of the netty library that block JNDI lookups using the following code
```java
package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.core.LogEvent;

public class JndiLookup implements StrLookup {
  public String lookup(String key) {
    return null;
  }
  
  public String lookup(LogEvent event, String key) {
    return null;
  }
}
```
Fun fact: This is also the library containing their code to blacklist servers