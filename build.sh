#!/bin/sh

DOCKER_IMAGE=maven:3.6-jdk-8
PRJ_NAME=`basename "$PWD"`

docker run -it --rm \
-v "$HOME/.m2":/root/.m2 \
-v "$PWD":/usr/src/$PRJ_NAME \
-w /usr/src/$PRJ_NAME \
$DOCKER_IMAGE \
mvn package jib:buildTar && docker image load -i mysql/target/jib-image.tar

