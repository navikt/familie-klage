FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:e132f3f96eb8907608f3e8da3ae926a30fefc36c9b2a28cc9adf3720c29fdbeb
ENV TZ="Europe/Oslo"
COPY target/familie-klage.jar /app/app.jar
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75"
CMD ["-jar", "/app/app.jar"]
