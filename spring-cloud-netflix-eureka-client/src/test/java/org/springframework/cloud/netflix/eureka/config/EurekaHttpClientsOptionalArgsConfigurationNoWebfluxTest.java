/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka.config;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.eureka.sample.EurekaSampleApplication;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author Daniel Lavoie
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "jersey-client-*", "jersey-core-*", "jersey-apache-client4-*",
		"spring-webflux-*" })
@SpringBootTest(classes = EurekaSampleApplication.class,
		webEnvironment = WebEnvironment.RANDOM_PORT)
public class EurekaHttpClientsOptionalArgsConfigurationNoWebfluxTest {

	@Test
	@SuppressWarnings("unchecked")
	public void contextFailsWithoutWebClient() {

		ConfigurableApplicationContext ctx = null;
		try {
			TomcatURLStreamHandlerFactory.disable();
			ctx = new SpringApplicationBuilder(EurekaSampleApplication.class)
					.properties("eureka.client.webclient.enabled=true").run();
			fail("exception not thrown");
		}
		catch (Exception e) {
			// this is the desired state
			assertThat(e).hasMessageContaining("WebClient is not on the classpath");
		}
		if (ctx != null) {
			ctx.close();
		}
	}

}
