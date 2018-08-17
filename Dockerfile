FROM openjdk:8-jdk-alpine
VOLUME /tmp

# Note that the ./target directory is used for Adding the jar into the Docker Image
# When deploying into AWS Elastic Beanstalk, this Dockerfile and the jar are packaged into a zip
# so the Maven assembly plugin that creates the zip file should ensure the zip file is still in a ./target directory

ADD ./target/a2si-capacity-info-reader-0.0.17-SNAPSHOT.jar a2si-capacity-info-reader.jar
ADD ./keystore.jks keystore.jks

# Expose 7050, the default port used for Capacity Info Reader
EXPOSE 7050
ENV JAVA_OPTS=""
RUN ["apk", "update"]
RUN ["apk", "add", "tzdata"]
RUN ["ln", "-f", "-s", "/usr/share/zoneinfo/Europe/London", "/etc/localtime"]
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar a2si-capacity-info-reader.jar" ]
