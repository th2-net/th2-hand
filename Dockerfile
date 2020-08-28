FROM gradle:6.4-jdk11 AS build
COPY ./ .
RUN gradle dockerPrepare

FROM openjdk:12-alpine
ENV GRPC_PORT=8080\
    RH_URL="http://localhost:8008"
WORKDIR /home
COPY --from=build /home/gradle/build/docker ./
ENTRYPOINT ["/home/th2-hand/bin/th2-hand"]