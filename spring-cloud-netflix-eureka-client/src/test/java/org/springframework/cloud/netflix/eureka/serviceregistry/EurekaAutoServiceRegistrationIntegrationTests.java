/*
 * Copyright 2013-2023 the original author or authors.
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

package org.springframework.cloud.netflix.eureka.serviceregistry;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.discovery.event.InstancePreRegisteredEvent;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link EurekaAutoServiceRegistration}.
 *
 * @author Olga Maciaszek-Sharma
 */
@SpringBootTest(classes = EurekaAutoServiceRegistrationIntegrationTests.Config.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaAutoServiceRegistrationIntegrationTests {

	@LocalServerPort
	private int serverPort;

	@Autowired
	private PreEventListener preEventListener;

	@Autowired
	private PostEventListener postEventListener;

	@Test
	void shouldPublishRegistrationEvents() {
		assertThat(preEventListener.wasFired).isTrue();
		assertThat(preEventListener.registration).isInstanceOf(EurekaRegistration.class);
		assertThat(preEventListener.registration.getPort()).isEqualTo(serverPort);
		assertThat(postEventListener.wasFired).isTrue();
		assertThat(postEventListener.config.getNonSecurePort()).isEqualTo(serverPort);
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	public static class Config {

		@Bean
		public PreEventListener preRegisterListener() {
			return new PreEventListener();
		}

		@Bean
		public PostEventListener postEventListener() {
			return new PostEventListener();
		}

	}

	public static class PreEventListener implements ApplicationListener<InstancePreRegisteredEvent> {

		public boolean wasFired = false;

		public Registration registration;

		@Override
		public void onApplicationEvent(InstancePreRegisteredEvent event) {
			this.registration = event.getRegistration();
			this.wasFired = true;
		}

	}

	public static class PostEventListener implements ApplicationListener<InstanceRegisteredEvent> {

		public boolean wasFired = false;

		public EurekaInstanceConfigBean config;

		@Override
		public void onApplicationEvent(InstanceRegisteredEvent event) {
			this.config = (EurekaInstanceConfigBean) event.getConfig();
			this.wasFired = true;
		}

	}

}
