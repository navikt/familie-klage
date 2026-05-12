FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:c5aacb7577de3c54fadf8184f18f749cb4c82fa878c4aaa9d553fa79983dbce0
ENV TZ="Europe/Oslo"
COPY target/familie-klage.jar /app/app.jar
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75"
CMD ["-jar", "/app/app.jar"]
