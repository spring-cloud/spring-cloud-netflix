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
 */

package org.springframework.cloud.netflix.hystrix.stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest("eureka.client.enabled=false")
@DirtiesContext
public class HystrixStreamAutoConfigurationNoRegistrationTests {

	@Autowired
	HystrixStreamTask task;

	@Autowired(required = false)
	Registration registration;

	@Autowired
	SimpleDiscoveryProperties simpleDiscoveryProperties;

	@Test
	public void withoutRegistrationWorks() throws Exception {
		assertThat(this.registration).isNull();
		assertThat(this.simpleDiscoveryProperties).isNotNull();
		assertThat(task.getRegistration()).isEqualTo(this.simpleDiscoveryProperties.getLocal());
	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	protected static class Config {
	}

}
