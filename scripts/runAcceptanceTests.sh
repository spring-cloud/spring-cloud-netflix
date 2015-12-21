#!/bin/bash

set -o errexit

mkdir -p target

SCRIPT_URL="https://raw.githubusercontent.com/spring-cloud-samples/brewery/master/acceptance-tests/scripts/runDockerAcceptanceTests.sh"
AT_WHAT_TO_TEST="EUREKA"
TEST_OPTS="-Dpresenting.poll.interval=5"

cd target

curl "${SCRIPT_URL}" --output runDockerAcceptanceTests.sh

chmod +x runDockerAcceptanceTests.sh

./runDockerAcceptanceTests.sh -t "${AT_WHAT_TO_TEST}" -o "${TEST_OPTS}"

SCRIPT_URL="https://raw.githubusercontent.com/spring-cloud-samples/tests/master/scripts/runTests.sh"

curl "${SCRIPT_URL}" --output runIntegrationTests.sh

chmod +x runIntegrationTests.sh

./runIntegrationTests.sh
