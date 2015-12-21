#!/bin/bash

set -o errexit

mkdir -p target

SCRIPT_URL="https://raw.githubusercontent.com/spring-cloud-samples/brewery/master/acceptance-tests/scripts/runDockerAcceptanceTests.sh"
AT_WHAT_TO_TEST="EUREKA"

cd target

curl "${SCRIPT_URL}" --output runDockerAcceptanceTests.sh

chmod +x runDockerAcceptanceTests.sh

./runDockerAcceptanceTests.sh -t "${AT_WHAT_TO_TEST}"

SCRIPT_URL="https://raw.githubusercontent.com/spring-cloud-samples/tests/master/scripts/runTests.sh"

curl "${SCRIPT_URL}" --output runIntegrationTests.sh

chmod +x runIntegrationTests.sh

./runIntegrationTests.sh
