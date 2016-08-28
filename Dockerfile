FROM java:openjdk-8

ADD . /app
RUN chmod -R 777 /app
RUN cd /app && ./gradlew build
RUN cp /app/build/libs/hello-world-latest.jar /app.jar
RUN rm -rf /app

ENV SERVER_PORT=80
EXPOSE 80

VOLUME /tmp
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]
