FROM openjdk:8

ADD target/networkdiscoveryclient-0.0.1-SNAPSHOT.jar /app.jar
ADD startUp.sh /startUp.sh
ENTRYPOINT ["/bin/bash", "/startUp.sh"]