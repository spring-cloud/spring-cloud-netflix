/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.springframework.cloud.netflix.sidecar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.metadata.DefaultManagementMetadataProvider;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadata;
import org.springframework.cloud.netflix.eureka.metadata.ManagementMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableSidecar
@RestController
public class SidecarApplication {

	public static void main(String[] args) {
		SpringApplication.run(SidecarApplication.class, args);
	}

	@Bean
	public ManagementMetadataProvider managementMetadataProvider() {
		//The default management metadata provider checks for random ports, we dont care about this in tests
		return new DefaultManagementMetadataProvider() {
			@Override
			public ManagementMetadata get(EurekaInstanceConfigBean instance, int serverPort, String serverContextPath, String managementContextPath, Integer managementPort) {
				String healthCheckUrl = getHealthCheckUrl(instance, serverPort, serverContextPath,
						managementContextPath, managementPort, false);
				String statusPageUrl = getStatusPageUrl(instance, serverPort, serverContextPath,
						managementContextPath, managementPort);

				ManagementMetadata metadata = new ManagementMetadata(healthCheckUrl, statusPageUrl, managementPort == null ? serverPort : managementPort);
				if(instance.isSecurePortEnabled()) {
					metadata.setSecureHealthCheckUrl(getHealthCheckUrl(instance, serverPort, serverContextPath,
							managementContextPath, managementPort, true));
				}
				return metadata;
			}
		};
	}

}
