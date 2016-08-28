FROM java:openjdk-8

VOLUME /tmp
ADD . /tmp

RUN cd /tmp && chmod +x gradlew && ./gradlew build
RUN cp /tmp/build/libs/hello-world-latest.jar /app.jar
RUN touch /app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]
