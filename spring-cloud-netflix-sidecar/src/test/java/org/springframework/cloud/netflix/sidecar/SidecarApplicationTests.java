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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

public class SidecarApplicationTests {

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = SidecarApplication.class, webEnvironment = RANDOM_PORT,
			properties = { "spring.application.name=mytest",
					"spring.cloud.client.hostname=mhhost",
					"spring.application.instance_id=1", "eureka.instance.hostname=mhhost",
					"sidecar.port=7000", "sidecar.ip-address=127.0.0.1" })
	public static class EurekaTestConfigBeanTest {

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

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = SidecarApplication.class, webEnvironment = RANDOM_PORT,
			properties = { "spring.application.name=mytest",
					"spring.cloud.client.hostname=mhhost",
					"spring.application.instance_id=1", "sidecar.hostname=mhhost",
					"sidecar.port=7000", "sidecar.ip-address=127.0.0.1" })
	public static class NewPropertyEurekaTestConfigBeanTest {

		@Autowired
		EurekaInstanceConfigBean config;

		@Test
		public void testEurekaConfigBean() {
			assertThat(config.getAppname()).isEqualTo("mytest");
			assertThat(config.getHostname()).isEqualTo("mhhost");
			assertThat(config.getInstanceId()).isEqualTo("mhhost:mytest:1");
			assertThat(config.getNonSecurePort()).isEqualTo(7000);
		}

	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = SidecarApplication.class, webEnvironment = RANDOM_PORT,
			properties = { "spring.application.name=mytest",
					"spring.cloud.client.hostname=mhhost",
					"spring.application.instance_id=1",
					"eureka.instance.hostname=mhhost1", "sidecar.hostname=mhhost2",
					"sidecar.port=7000", "sidecar.ip-address=127.0.0.1" })
	public static class BothPropertiesEurekaTestConfigBeanTest {

		@Autowired
		EurekaInstanceConfigBean config;

		@Test
		public void testEurekaConfigBeanEurekaInstanceHostnamePropertyShouldBeUsed() {
			assertThat(config.getAppname()).isEqualTo("mytest");
			assertThat(config.getHostname()).isEqualTo("mhhost1");
			assertThat(config.getInstanceId()).isEqualTo("mhhost:mytest:1");
			assertThat(config.getNonSecurePort()).isEqualTo(7000);
		}

	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = SidecarApplication.class, webEnvironment = RANDOM_PORT,
			properties = { "spring.application.name=mytest",
					"spring.cloud.client.hostname=mhhost",
					"spring.application.instance_id=1",
					"eureka.instance.hostname=mhhost1", "sidecar.hostname=mhhost2",
					"sidecar.port=7000", "sidecar.ip-address=10.0.0.1",
					"eureka.instance.prefer-ip-address=true" })
	public static class PreferIpAddressTest {

		@Autowired
		EurekaInstanceConfigBean config;

		@Test
		public void testEurekaConfigBeanPreferIpAddress() {
			assertThat(config.getAppname()).isEqualTo("mytest");
			assertThat(config.getHostname()).isEqualTo("10.0.0.1");
			assertThat(config.getInstanceId()).isEqualTo("mhhost:mytest:1");
			assertThat(config.getNonSecurePort()).isEqualTo(7000);
		}

	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = SidecarApplication.class, webEnvironment = RANDOM_PORT,
			value = { "spring.application.name=mytest",
					"spring.cloud.client.hostname=mhhost",
					"spring.application.instance_id=1",
					"eureka.instance.hostname=mhhost1", "sidecar.hostname=mhhost2",
					"sidecar.port=7000", "sidecar.ipAddress=127.0.0.1",
					"management.context-path=/foo" })
	public static class ManagementContextPathStatusAndHealthCheckUrls {

		@Autowired
		EurekaInstanceConfigBean config;

		public void testStatusAndHealthCheckUrls() {
			assertThat(config.getStatusPageUrl()).isEqualTo("https://mhhost2:0/foo/info");
			assertThat(config.getHealthCheckUrl())
					.isEqualTo("https://mhhost2:0/foo/health");
		}

	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = SidecarApplication.class, webEnvironment = RANDOM_PORT,
			value = { "spring.application.name=mytest",
					"spring.cloud.client.hostname=mhhost",
					"spring.application.instance_id=1",
					"eureka.instance.hostname=mhhost1", "sidecar.hostname=mhhost2",
					"sidecar.port=7000", "sidecar.ipAddress=127.0.0.1",
					"server.context-path=/foo" })
	public static class ServerContextPathStatusAndHealthCheckUrls {

		@Autowired
		EurekaInstanceConfigBean config;

		@Test
		public void testStatusAndHealthCheckUrls() {
			assertThat(config.getStatusPageUrl()).isEqualTo("https://mhhost2:0/foo/info");
			assertThat(config.getHealthCheckUrl())
					.isEqualTo("https://mhhost2:0/foo/health");
		}

	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = SidecarApplication.class, webEnvironment = RANDOM_PORT,
			value = { "sidecar.accept-all-ssl-certificates=false" })
	public static class AcceptAllSslCertificatesContext {

		@Autowired
		RestTemplate restTemplate;

		@Test
		public void testUseRestTemplateWhenHttpClientIsNotAvailable() {
			assertThat(restTemplate.getRequestFactory()).isNull();
		}

	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = SidecarApplication.class, webEnvironment = RANDOM_PORT,
			value = { "sidecar.port=7000", "sidecar.ip-address=127.0.0.1",
					"sidecar.secure-port-enabled=true" })
	public static class SecurePortEnabled {

		@Autowired
		EurekaInstanceConfigBean config;

		@Test
		public void testThatSecureEnabledOptionIsSetFromPropertyFile() {
			assertThat(this.config.isSecurePortEnabled()).isEqualTo(true);
		}

	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = EurekaInstanceConfigBeanOverrideApplication.class,
			webEnvironment = RANDOM_PORT)
	public static class EurekaInstanceConfigBeanOverrideTest {

		@Autowired
		EurekaInstanceConfigBean config;

		@Test
		public void testEurekaConfigBeanOverride() {
			assertThat(this.config.getHostname()).isEqualTo("overridden");
		}

	}

	@Configuration
	@EnableAutoConfiguration
	@EnableSidecar
	protected static class EurekaInstanceConfigBeanOverrideApplication {

		@Bean
		public EurekaInstanceConfigBean eurekaInstanceConfigBean(InetUtils inetUtils) {
			EurekaInstanceConfigBean eurekaInstanceConfigBean = new EurekaInstanceConfigBean(
					inetUtils);
			eurekaInstanceConfigBean.setHostname("overridden");
			return eurekaInstanceConfigBean;
		}

	}

}
