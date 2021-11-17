#!/bin/bash

set -o errexit

cd spring-cloud-netflix-hystrix-contract
../mvnw clean install -B -Pdocs ${@}
cd ..
./mvnw clean install -B -Pdocs ${@}
