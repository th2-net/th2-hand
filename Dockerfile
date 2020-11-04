FROM gradle:6.6-jdk11 AS build
ARG app_version=0.0.0
COPY ./ .
RUN gradle dockerPrepare -Prelease_version=${app_version}

FROM openjdk:12-alpine
ENV GRPC_PORT=8080\
    DRIVERS_MAPPING="Default=web@http://localhost:4444"
WORKDIR /home
COPY --from=build /home/gradle/build/docker ./
ENTRYPOINT ["/home/service/bin/service"]