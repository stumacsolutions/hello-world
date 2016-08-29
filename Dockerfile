FROM frolvlad/alpine-oraclejdk8:slim
VOLUME /tmp

ADD hello-world-latest.jar app.jar
RUN sh -c 'touch /app.jar'

ENV SERVER_PORT=80
EXPOSE 80

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
