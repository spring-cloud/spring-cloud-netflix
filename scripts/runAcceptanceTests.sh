#!/bin/bash

set -o errexit

mkdir -p target

SCRIPT_URL="https://raw.githubusercontent.com/spring-cloud-samples/brewery/removing_docker_compose_zookeeper/runAcceptanceTests.sh"
AT_WHAT_TO_TEST="EUREKA"

cd target

curl "${SCRIPT_URL}" --output runAcceptanceTests.sh

chmod +x runAcceptanceTests.sh

./runAcceptanceTests.sh -t "${AT_WHAT_TO_TEST}" -k 1

SCRIPT_URL="https://raw.githubusercontent.com/spring-cloud-samples/tests/master/scripts/runTests.sh"

curl "${SCRIPT_URL}" --output runIntegrationTests.sh

chmod +x runIntegrationTests.sh

./runIntegrationTests.sh
