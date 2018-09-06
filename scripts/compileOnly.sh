#!/bin/bash

(cd spring-cloud-netflix-hystrix-contract && ../mvnw clean install -B -Pdocs -DskipTests)
./mvnw clean install -B -Pdocs -DskipTests -fae
