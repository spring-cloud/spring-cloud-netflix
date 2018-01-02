#!/bin/bash

(cd spring-cloud-netflix-hystrix-contract && ../mvnw clean install -B -Pdocs ${@})
./mvnw clean install -B -Pdocs ${@}
