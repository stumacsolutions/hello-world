FROM java:openjdk-8

ADD . /app
RUN chmod -R 777 /app
RUN cd /app && ./gradlew build
RUN cp /app/build/libs/hello-world-latest.jar /app.jar
RUN rm -rf /app

VOLUME /tmp
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]
