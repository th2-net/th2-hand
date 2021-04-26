FROM adoptopenjdk/openjdk11:alpine as libwebp
ARG libwebp_version=v1.2.0
WORKDIR /home
RUN apk add --no-cache make git gcc musl-dev swig \
    && git clone --branch ${libwebp_version} --single-branch https://chromium.googlesource.com/webm/libwebp  \
    && cd libwebp \
    && sed -i '17,20d' makefile.unix \
    && make -f makefile.unix CPPFLAGS="-I. -Isrc/ -Wall -fPIC" \
    && cd swig \
    && mkdir -p java/com/exactpro/remotehand/screenwriter \
    && swig -java -package com.exactpro.remotehand.screenwriter \
        -outdir java/com/exactpro/remotehand/screenwriter -o libwebp_java_wrap.c libwebp.swig \
    && gcc -shared -fPIC -fno-strict-aliasing -O2 -I/opt/java/openjdk/include/ -I/opt/java/openjdk/include/linux \
        -I../src -L../src libwebp_java_wrap.c -lwebp -o libwebp.so

FROM gradle:6.6-jdk11 AS build
ARG app_version=0.0.0
COPY ./ .
RUN gradle dockerPrepare -Prelease_version=${app_version}

FROM adoptopenjdk/openjdk11:alpine
WORKDIR /home
COPY --from=libwebp /home/libwebp/swig/libwebp.so ./
COPY --from=build /home/gradle/build/docker ./
ENTRYPOINT ["/home/service/bin/service"]
