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

package org.springframework.cloud.netflix.eureka;

import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
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
public class EurekaInstanceConfigBeanTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	private String hostName;

	private String ipAddress;

	@Before
	public void init() throws Exception {
		try (InetUtils utils = new InetUtils(new InetUtilsProperties())) {
			InetUtils.HostInfo hostInfo = utils.findFirstNonLoopbackHostInfo();
			this.hostName = hostInfo.getHostname();
			this.ipAddress = hostInfo.getIpAddress();
		}
	}

	@After
	public void clear() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void basicBinding() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup")
				.applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getAppGroupName()).isEqualTo("mygroup");
	}

	@Test
	public void nonSecurePort() {
		TestPropertyValues.of("eureka.instance.nonSecurePort:8888").applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getNonSecurePort()).isEqualTo(8888);
	}

	@Test
	public void instanceId() {
		TestPropertyValues.of("eureka.instance.instanceId:special").applyTo(this.context);
		setupContext();
		EurekaInstanceConfigBean instance = getInstanceConfig();
		assertThat(instance.getInstanceId()).isEqualTo("special");
	}

	@Test
	public void initialHostName() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup")
				.applyTo(this.context);
		setupContext();
		if (this.hostName != null) {
			assertThat(getInstanceConfig().getHostname()).isEqualTo(this.hostName);
		}
	}

	@Test
	public void refreshHostName() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup")
				.applyTo(this.context);
		setupContext();
		ReflectionTestUtils.setField(getInstanceConfig(), "hostname", "marvin");
		assertThat(getInstanceConfig().getHostname()).isEqualTo("marvin");
		getInstanceConfig().getHostName(true);
		if (this.hostName != null) {
			assertThat(getInstanceConfig().getHostname()).isEqualTo(this.hostName);
		}
	}

	@Test
	public void refreshHostNameWhenSetByUser() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup")
				.applyTo(this.context);
		setupContext();
		getInstanceConfig().setHostname("marvin");
		assertThat(getInstanceConfig().getHostname()).isEqualTo("marvin");
		getInstanceConfig().getHostName(true);
		assertThat(getInstanceConfig().getHostname()).isEqualTo("marvin");
	}

	@Test
	public void initialIpAddress() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup")
				.applyTo(this.context);
		setupContext();
		if (this.ipAddress != null) {
			assertThat(getInstanceConfig().getIpAddress()).isEqualTo(this.ipAddress);
		}
	}

	@Test
	public void refreshIpAddress() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup")
				.applyTo(this.context);
		setupContext();
		ReflectionTestUtils.setField(getInstanceConfig(), "ipAddress", "10.0.0.1");
		assertThat(getInstanceConfig().getIpAddress()).isEqualTo("10.0.0.1");
		getInstanceConfig().getHostName(true);
		if (this.ipAddress != null) {
			assertThat(getInstanceConfig().getIpAddress()).isEqualTo(this.ipAddress);
		}
	}

	@Test
	public void refreshIpAddressWhenSetByUser() {
		TestPropertyValues.of("eureka.instance.appGroupName=mygroup")
				.applyTo(this.context);
		setupContext();
		getInstanceConfig().setIpAddress("10.0.0.1");
		assertThat(getInstanceConfig().getIpAddress()).isEqualTo("10.0.0.1");
		getInstanceConfig().getHostName(true);
		assertThat(getInstanceConfig().getIpAddress()).isEqualTo("10.0.0.1");
	}

	@Test
	public void testDefaultInitialStatus() {
		setupContext();
		assertThat(getInstanceConfig().getInitialStatus()).as("initialStatus wrong")
				.isEqualTo(InstanceStatus.UP);
	}

	@Test(expected = BeanCreationException.class)
	public void testBadInitialStatus() {
		TestPropertyValues.of("eureka.instance.initial-status:FOO").applyTo(this.context);
		setupContext();
	}

	@Test
	public void testCustomInitialStatus() {
		TestPropertyValues.of("eureka.instance.initial-status:STARTING")
				.applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getInitialStatus()).as("initialStatus wrong")
				.isEqualTo(InstanceStatus.STARTING);
	}

	@Test
	public void testPreferIpAddress() throws Exception {
		TestPropertyValues.of("eureka.instance.preferIpAddress:true")
				.applyTo(this.context);
		setupContext();
		EurekaInstanceConfigBean instance = getInstanceConfig();
		assertThat(getInstanceConfig().getHostname().equals(instance.getIpAddress()))
				.as("Wrong hostname: " + instance.getHostname()).isTrue();

	}

	@Test
	public void testDefaultVirtualHostName() throws Exception {
		TestPropertyValues.of("spring.application.name:myapp").applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getVirtualHostName()).as("virtualHostName wrong")
				.isEqualTo("myapp");
		assertThat(getInstanceConfig().getSecureVirtualHostName())
				.as("secureVirtualHostName wrong").isEqualTo("myapp");

	}

	@Test
	public void testCustomVirtualHostName() throws Exception {
		TestPropertyValues
				.of("spring.application.name:myapp",
						"eureka.instance.virtualHostName=myvirthost",
						"eureka.instance.secureVirtualHostName=mysecurevirthost")
				.applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getVirtualHostName()).as("virtualHostName wrong")
				.isEqualTo("myvirthost");
		assertThat(getInstanceConfig().getSecureVirtualHostName())
				.as("secureVirtualHostName wrong").isEqualTo("mysecurevirthost");

	}

	@Test
	public void testDefaultAppName() throws Exception {
		setupContext();
		assertThat(getInstanceConfig().getAppname()).as("default app name is wrong")
				.isEqualTo("unknown");
		assertThat(getInstanceConfig().getVirtualHostName())
				.as("default virtual hostname is wrong").isEqualTo("unknown");
		assertThat(getInstanceConfig().getSecureVirtualHostName())
				.as("default secure virtual hostname is wrong").isEqualTo("unknown");
	}

	@Test
	public void testCustomInstanceId() throws Exception {
		TestPropertyValues.of("eureka.instance.instanceId=myinstance")
				.applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getInstanceId()).as("instance id is wrong")
				.isEqualTo("myinstance");
	}

	@Test
	public void testCustomInstanceIdWithMetadata() throws Exception {
		TestPropertyValues.of("eureka.instance.metadataMap.instanceId=myinstance")
				.applyTo(this.context);
		setupContext();
		assertThat(getInstanceConfig().getInstanceId()).as("instance id is wrong")
				.isEqualTo("myinstance");
	}

	@Test
	public void testDefaultInstanceId() throws Exception {
		setupContext();
		assertThat(getInstanceConfig().getInstanceId()).as("default instance id is wrong")
				.isEqualTo(null);
	}

	private void setupContext() {
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
	}

	private EurekaInstanceConfigBean getInstanceConfig() {
		return this.context.getBean(EurekaInstanceConfigBean.class);
	}

	@Configuration
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
