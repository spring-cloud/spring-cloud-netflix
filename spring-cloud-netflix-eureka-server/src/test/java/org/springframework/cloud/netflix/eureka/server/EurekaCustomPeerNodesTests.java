/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.server;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.ServerCodecs;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EurekaCustomPeerNodesTests.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, value = {
		"spring.application.name=eureka", "server.contextPath=/context",
		"management.security.enabled=false" })
public class EurekaCustomPeerNodesTests {

	 @Autowired
	 private PeerEurekaNodes peerEurekaNodes;

	@Test
	public void testCustomPeerNodesShouldTakePrecedenceOverDefault() {
		assertTrue("PeerEurekaNodes should be the user created one",
				peerEurekaNodes instanceof CustomEurekaPeerNodes);
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableEurekaServer
	protected static class Application {

		public static void main(String[] args) {
			new SpringApplicationBuilder(ApplicationContextTests.Application.class)
					.properties("spring.application.name=eureka",
							"server.contextPath=/context")
					.run(args);
		}

		@Bean
		public PeerEurekaNodes myPeerEurekaNodes(PeerAwareInstanceRegistry registry,
				EurekaServerConfig eurekaServerConfig,
				EurekaClientConfig eurekaClientConfig, ServerCodecs serverCodecs,
				ApplicationInfoManager applicationInfoManager) {
			return new CustomEurekaPeerNodes(registry, eurekaServerConfig,
					eurekaClientConfig, serverCodecs, applicationInfoManager);
		}

	}

	private static class CustomEurekaPeerNodes extends PeerEurekaNodes {

		public CustomEurekaPeerNodes(PeerAwareInstanceRegistry registry,
				EurekaServerConfig serverConfig, EurekaClientConfig clientConfig,
				ServerCodecs serverCodecs,
				ApplicationInfoManager applicationInfoManager) {
			super(registry, serverConfig, clientConfig, serverCodecs,
					applicationInfoManager);
		}
	}

}
