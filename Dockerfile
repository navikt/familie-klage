FROM ghcr.io/navikt/baseimages/temurin:21

ENV APP_NAME=familie-klage
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

COPY ./target/familie-klage.jar "app.jar"
