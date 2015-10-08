package org.springframework.cloud.netflix.eureka;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.EnvironmentTestUtils.addEnvironment;

import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.netflix.appinfo.InstanceInfo;

public class InstanceInfoFactoryTests {
	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void instanceIdIsHostNameByDefault() {
		assertEquals(EurekaInstanceConfigBean.getFirstNonLoopbackAddress().getHostName(),
				setupInstance().getId());
	}

	@Test
	public void instanceIdIsIpWhenIpPreferred() throws Exception {
		assertTrue(setupInstance("eureka.instance.preferIpAddress:true").getId().matches(
				"(\\d+\\.){3}\\d+"));
	}

	@Test
	public void instanceIdIsSidWhenSet() {
		assertEquals("special", setupInstance("eureka.instance.sid:special").getId());
	}

	private InstanceInfo setupInstance(String... pairs) {
		for (String pair : pairs)
			addEnvironment(this.context, pair);

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
	@EnableConfigurationProperties(EurekaInstanceConfigBean.class)
	protected static class TestConfiguration {
	}
}
