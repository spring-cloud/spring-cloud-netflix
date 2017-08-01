package org.springframework.cloud.netflix.eureka;

import java.io.IOException;

import org.junit.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.InstanceInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.util.EnvironmentTestUtils.addEnvironment;

public class InstanceInfoFactoryTests {
	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void instanceIdIsHostNameByDefault() throws IOException {
		InstanceInfo instanceInfo = setupInstance();
		try (InetUtils utils = new InetUtils(new InetUtilsProperties())) {
			assertEquals(utils.findFirstNonLoopbackHostInfo().getHostname(),
					instanceInfo.getId());
		}
	}

	@Test
	public void instanceIdIsIpWhenIpPreferred() throws Exception {
		InstanceInfo instanceInfo = setupInstance("eureka.instance.preferIpAddress:true");
		assertTrue(instanceInfo.getId().matches("(\\d+\\.){3}\\d+"));
	}

	@Test
	public void instanceInfoIdIsInstanceIdWhenSet() {
		InstanceInfo instanceInfo = setupInstance("eureka.instance.instanceId:special");
		assertEquals("special", instanceInfo.getId());
	}

	private InstanceInfo setupInstance(String... pairs) {
		for (String pair : pairs) {
			addEnvironment(this.context, pair);
		}

		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();

		EurekaInstanceConfigBean instanceConfig = getInstanceConfig();
		return new InstanceInfoFactory().create(instanceConfig);
	}

	private EurekaInstanceConfigBean getInstanceConfig() {
		return this.context.getBean(EurekaInstanceConfigBean.class);
	}

	@Configuration
	@EnableConfigurationProperties
	protected static class TestConfiguration {
		@Bean
		public EurekaInstanceConfigBean eurekaInstanceConfigBean() {
			return new EurekaInstanceConfigBean(new InetUtils(new InetUtilsProperties()));
		}
	}
}
