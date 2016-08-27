FROM frolvlad/alpine-oraclejdk8:slim

ADD . /app
RUN sh -c '/app/gradlew.sh build'
RUN sh -c 'cp /app/build/libs/hello-world-latest.jar /app.jar'
RUN sh -c 'touch /app.jar'

VOLUME /tmp
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
