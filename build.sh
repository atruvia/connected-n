#!/bin/sh

DOCKER_IMAGE=maven:3.6-jdk-8
PRJ_NAME=`basename "$PWD"`

docker run -it --rm -u "$(id -u)" \
-v "$PWD":/usr/src/$PRJ_NAME \
-v "$HOME/.m2":/root/.m2 \
-w /usr/src/$PRJ_NAME \
$DOCKER_IMAGE \
mvn clean package jib:buildTar && docker image load -i target/jib-image.tar
