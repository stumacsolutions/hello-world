FROM java:openjdk-8

VOLUME /tmp

ADD . /app
RUN cd /app && chmod +x gradlew && ./gradlew build
RUN cp /app/build/libs/hello-world-latest.jar /app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]
