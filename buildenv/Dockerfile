FROM maven:3-jdk-8

USER root

ENV http_proxy=http://proxy01-ka.noc.fiducia.de:8080 \
    https_proxy=http://proxy01-ka.noc.fiducia.de:8080 \
    no_proxy="127.0.0.0/24,10.0.0.0/24,192.168.0.0/16,fiducia.de,fiduciagad.de,gad.de,10.2.181.249"

RUN apt-get update && apt-get install -y \
    curl \
    libltdl-dev

RUN mkdir -p /usr/local/share/ca-certificates/fiducia.de/ \
    && curl http://arilux.campus-ka.fiducia.de/certs.tar.gz -o certs.tar.gz \
    && tar -zxvf certs.tar.gz -C /usr/local/share/ca-certificates/fiducia.de \
    && rm certs.tar.gz \
    && update-ca-certificates

COPY ./maven-settings.xml $MAVEN_CONFIG/settings.xml
