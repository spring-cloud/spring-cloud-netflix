#!/bin/bash

(cd spring-cloud-netflix-hystrix-contract && ../mvnw clean -DskipTests=true install -B -Pdocs ${@})
./mvnw clean -DskipTests=true install -B -Pdocs ${@}
