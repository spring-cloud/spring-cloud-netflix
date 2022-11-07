/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.cloud.netflix.eureka;

import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 * @author Ryan Baxter
 * @author Tim Ysewyn
 */
class EurekaInstanceConfigBeanTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	private String hostName;

	private String ipAddress;

	@BeforeEach
	void init() {
		try (InetUtils utils = new InetUtils(new InetUtilsProperties())) {
			InetUtils.HostInfo hostInfo = utils.findFirstNonLoopbackHostInfo();
			this.hostName = hostInfo.getHostname();
			this.ipAddress = hostInfo.getIpAddress();
		}
	}

	@AfterEach
	void clear() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void basicBinding() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup").applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getAppGroupName()).isEqualTo("mygroup");
	}

	@Test
	void nonSecurePort() {
		TestPropertyValues.of("eureka.instance.nonSecurePort:8888").applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getNonSecurePort()).isEqualTo(8888);
	}

	@Test
	void instanceId() {
		TestPropertyValues.of("eureka.instance.instanceId:special").applyTo(this.context);
		setupContext();
		EurekaInstanceConfigBean instance = getInstanceConfig();
		assertThat(instance.getInstanceId()).isEqualTo("special");
	}

	@Test
	void initialHostName() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup").applyTo(this.context);
		setupContext();
		if (this.hostName != null) {
			assertThat(getInstanceConfig().getHostname()).isEqualTo(this.hostName);
		}
	}

	@Test
	void refreshHostName() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup").applyTo(this.context);
		setupContext();
		ReflectionTestUtils.setField(getInstanceConfig(), "hostname", "marvin");
		assertThat(getInstanceConfig().getHostname()).isEqualTo("marvin");
		getInstanceConfig().getHostName(true);
		if (this.hostName != null) {
			assertThat(getInstanceConfig().getHostname()).isEqualTo(this.hostName);
		}
	}

	@Test
	void refreshHostNameWhenSetByUser() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup").applyTo(this.context);
		setupContext();
		getInstanceConfig().setHostname("marvin");
		assertThat(getInstanceConfig().getHostname()).isEqualTo("marvin");
		getInstanceConfig().getHostName(true);
		assertThat(getInstanceConfig().getHostname()).isEqualTo("marvin");
	}

	@Test
	void initialIpAddress() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup").applyTo(this.context);
		setupContext();
		if (this.ipAddress != null) {
			assertThat(getInstanceConfig().getIpAddress()).isEqualTo(this.ipAddress);
		}
	}

	@Test
	void refreshIpAddress() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup").applyTo(this.context);
		setupContext();
		ReflectionTestUtils.setField(getInstanceConfig(), "ipAddress", "10.0.0.1");
		assertThat(getInstanceConfig().getIpAddress()).isEqualTo("10.0.0.1");
		getInstanceConfig().getHostName(true);
		if (this.ipAddress != null) {
			assertThat(getInstanceConfig().getIpAddress()).isEqualTo(this.ipAddress);
		}
	}

	@Test
	void refreshIpAddressWhenSetByUser() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup").applyTo(this.context);
		setupContext();
		getInstanceConfig().setIpAddress("10.0.0.1");
		assertThat(getInstanceConfig().getIpAddress()).isEqualTo("10.0.0.1");
		getInstanceConfig().getHostName(true);
		assertThat(getInstanceConfig().getIpAddress()).isEqualTo("10.0.0.1");
	}

	@Test
	void testDefaultInitialStatus() {
		setupContext();
		assertThat(getInstanceConfig().getInitialStatus()).as("initialStatus wrong").isEqualTo(InstanceStatus.UP);
	}

	@Test
	void testCustomInitialStatus() {
		TestPropertyValues.of("eureka.instance.initial-status:STARTING").applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getInitialStatus()).as("initialStatus wrong").isEqualTo(InstanceStatus.STARTING);
	}

	@Test
	void testPreferIpAddress() {
		TestPropertyValues.of("eureka.instance.preferIpAddress:true").applyTo(this.context);
		setupContext();
		EurekaInstanceConfigBean instance = getInstanceConfig();
		assertThat(getInstanceConfig().getHostname().equals(instance.getIpAddress()))
				.as("Wrong hostname: " + instance.getHostname()).isTrue();

	}

	@Test
	void testDefaultVirtualHostName() {
		TestPropertyValues.of("spring.application.name:myapp").applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getVirtualHostName()).as("virtualHostName wrong").isEqualTo("myapp");
		assertThat(getInstanceConfig().getSecureVirtualHostName()).as("secureVirtualHostName wrong").isEqualTo("myapp");

	}

	@Test
	void testCustomVirtualHostName() {
		TestPropertyValues.of("spring.application.name:myapp", "eureka.instance.virtualHostName=myvirthost",
				"eureka.instance.secureVirtualHostName=mysecurevirthost").applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getVirtualHostName()).as("virtualHostName wrong").isEqualTo("myvirthost");
		assertThat(getInstanceConfig().getSecureVirtualHostName()).as("secureVirtualHostName wrong")
				.isEqualTo("mysecurevirthost");

	}

	@Test
	void testDefaultAppName() {
		setupContext();
		assertThat(getInstanceConfig().getAppname()).as("default app name is wrong").isEqualTo("unknown");
		assertThat(getInstanceConfig().getVirtualHostName()).as("default virtual hostname is wrong")
				.isEqualTo("unknown");
		assertThat(getInstanceConfig().getSecureVirtualHostName()).as("default secure virtual hostname is wrong")
				.isEqualTo("unknown");
	}

	@Test
	void testCustomInstanceId() {
		TestPropertyValues.of("eureka.instance.instanceId=myinstance").applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getInstanceId()).as("instance id is wrong").isEqualTo("myinstance");
	}

	@Test
	void testCustomInstanceIdWithMetadata() {
		TestPropertyValues.of("eureka.instance.metadataMap.instanceId=myinstance").applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getInstanceId()).as("instance id is wrong").isEqualTo("myinstance");
	}

	@Test
	void testDefaultInstanceId() {
		setupContext();
		assertThat(getInstanceConfig().getInstanceId()).as("default instance id is wrong").isEqualTo(null);
	}

	private void setupContext() {
		this.context.register(PropertyPlaceholderAutoConfiguration.class, TestConfiguration.class);
		this.context.refresh();
	}

	private EurekaInstanceConfigBean getInstanceConfig() {
		return this.context.getBean(EurekaInstanceConfigBean.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	protected static class TestConfiguration {

		@Autowired
		ConfigurableEnvironment env;

		@Bean
		public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
			EurekaInstanceConfigBean configBean = new EurekaInstanceConfigBean(
					new InetUtils(new InetUtilsProperties()));
			String springAppName = this.env.getProperty("spring.application.name", "");
			if (StringUtils.hasText(springAppName)) {
				configBean.setSecureVirtualHostName(springAppName);
				configBean.setVirtualHostName(springAppName);
				configBean.setAppname(springAppName);
			}
			return configBean;
		}

	}

}
