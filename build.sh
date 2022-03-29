#!/bin/sh

./mvnw package jib:buildTar && docker image load -i udp/target/jib-image.tar

