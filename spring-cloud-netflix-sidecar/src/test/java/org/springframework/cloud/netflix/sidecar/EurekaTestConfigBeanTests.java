/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.netflix.sidecar;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Ryan Baxter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SidecarApplication.class, webEnvironment = RANDOM_PORT,
		properties = { "spring.application.name=mytest",
				"spring.cloud.client.hostname=mhhost", "spring.application.instance_id=1",
				"eureka.instance.hostname=mhhost", "sidecar.port=7000",
				"sidecar.ip-address=127.0.0.1" })
public class EurekaTestConfigBeanTests {

	@Autowired
	EurekaInstanceConfigBean config;

	@Test
	public void testEurekaConfigBean() {
		assertThat(this.config.getAppname()).isEqualTo("mytest");
		assertThat(this.config.getHostname()).isEqualTo("mhhost");
		assertThat(this.config.getInstanceId()).isEqualTo("mhhost:mytest:1");
		assertThat(this.config.getNonSecurePort()).isEqualTo(7000);
	}

}
