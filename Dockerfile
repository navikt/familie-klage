FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:4ff3c87d282656fa0e7d33702a6db6063c7a4fd4ebc590fd42adb32949ba3ca4
ENV TZ="Europe/Oslo"
COPY target/familie-klage.jar /app/app.jar
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75"
CMD ["-jar", "/app/app.jar"]
