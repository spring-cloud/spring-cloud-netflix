#!/bin/bash

set -o errexit

mkdir -p target

SCRIPT_URL="https://raw.githubusercontent.com/spring-cloud-samples/brewery/master/acceptance-tests/scripts/runDockerAcceptanceTests.sh"
AT_WHAT_TO_TEST="EUREKA"
AT_VERSION="1.1.0.BUILD-SNAPSHOT"

cd target

curl "${SCRIPT_URL}" --output runDockerAcceptanceTests.sh

chmod +x runDockerAcceptanceTests.sh

./runDockerAcceptanceTests.sh -t "${AT_WHAT_TO_TEST}" -v "${AT_VERSION}"

SCRIPT_URL="https://raw.githubusercontent.com/spring-cloud-samples/tests/master/scripts/runTests.sh"

curl "${SCRIPT_URL}" --output runIntegrationTests.sh

chmod +x runIntegrationTests.sh

./runIntegrationTests.sh
