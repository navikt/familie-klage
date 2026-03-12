FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:54f25c28aa72a5f400c7691e4721fa733a7a504957dee05a07d2dd04971de613
ENV TZ="Europe/Oslo"
COPY target/familie-klage.jar /app/app.jar
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75"
CMD ["-jar", "/app/app.jar"]
