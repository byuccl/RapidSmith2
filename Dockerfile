FROM ubuntu:16.04
WORKDIR /rapidsmith2/
RUN apt-get -qq update && apt-get -qq install -y \
  openjdk-8-jdk \
  graphviz
ENV RAPIDSMITH_PATH /rapidsmith2
ENV CLASSPATH ${RAPIDSMITH_PATH}/build/install/RapidSmith2/*:${RAPIDSMITH_PATH}/build/install/RapidSmith2/lib/*
COPY . .
RUN ./gradlew build

CMD bash --init-file Dockerinit.sh
