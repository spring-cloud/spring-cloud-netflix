FROM maven:3.8.4-openjdk-8 as BUILD

SHELL ["/bin/bash", "-c"]

RUN apt-get update && apt-get install zip -y

RUN curl -s "https://get.sdkman.io" | bash

RUN source /root/.sdkman/bin/sdkman-init.sh && sdk install springboot

RUN apt-get install openjdk-17-jdk  -y

RUN update-java-alternatives -s java-1.17.0-openjdk-amd64 

RUN git clone https://github.com/spring-cloud/spring-cloud-netflix.git --depth 1

RUN export JAVA_HOME=/usr/lib/jvm/java-1.17.0-openjdk-amd64 && cd spring-cloud-netflix && ./mvnw install

# RUN spring initializr new \
#     --path demo \
#     --project gradle-project \
#     --language java \
#     --boot-version 2.7.0 \
#     --version 0.0.1-SNAPSHOT \
#     --group com.example \
#     --artifact demo \
#     --name demo \
#     --description "Demo project" \
#     --package-name com.example.demo \
#     --dependencies org.springframework.cloud:spring-cloud-starter-netflix-eureka-server \
#     --packaging jar \
#     --java-version 17   