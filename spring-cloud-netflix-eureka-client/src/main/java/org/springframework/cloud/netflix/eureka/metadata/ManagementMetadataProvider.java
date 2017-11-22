package org.springframework.cloud.netflix.eureka.metadata;

import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;

public interface ManagementMetadataProvider {

    ManagementMetadata get(EurekaInstanceConfigBean instance, int serverPort,
                           String serverContextPath, String managementContextPath, Integer managementPort);

}
