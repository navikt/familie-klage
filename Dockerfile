FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:4f0da573eff60b068101b5894040c0793346c4ad41c783960984664bdc22b4ba
ENV TZ="Europe/Oslo"
COPY target/familie-klage.jar /app/app.jar
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75"
CMD ["-jar", "/app/app.jar"]
