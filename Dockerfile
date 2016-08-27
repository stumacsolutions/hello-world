FROM java:openjdk-8

VOLUME /tmp
ADD . /tmp

RUN sh -c 'cd /tmp && chmod +x gradlew ./gradlew build --stacktrace'
RUN sh -c 'cp /tmp/build/libs/hello-world-latest.jar /app.jar'
RUN sh -c 'touch /app.jar'

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]
