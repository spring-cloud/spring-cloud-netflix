/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.cloud.netflix.eureka;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;

import com.netflix.appinfo.UniqueIdentifier;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;

import static org.junit.Assert.*;
import static org.springframework.boot.test.EnvironmentTestUtils.*;

/**
 * @author Dave Syer
 *
 */
public class EurekaInstanceConfigBeanTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void init() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void idFromInstanceId() throws Exception {
		EurekaInstanceConfigBean instance = new EurekaInstanceConfigBean();
		instance.getMetadataMap().put("instanceId", "foo");
		instance.setHostname("bar");
		assertEquals("bar:foo", ((UniqueIdentifier) instance.getDataCenterInfo()).getId());
	}

	@Test
	public void basicBinding() {
		addEnvironment(context, "eureka.instance.appGroupName=mygroup");
		setupContext();
		assertEquals("mygroup", getInstanceConfig().getAppGroupName());
	}

	@Test
	public void nonSecurePort() {
		testNonSecurePort("eureka.instance.nonSecurePort");
	}

	@Test
	public void nonSecurePort2() {
		testNonSecurePort("server.port");
	}

	@Test
	public void nonSecurePort3() {
		testNonSecurePort("SERVER_PORT");
	}

	@Test
	public void nonSecurePort4() {
		testNonSecurePort("PORT");
	}

	private void testNonSecurePort(String propName) {
		addEnvironment(context, propName + ":8888");
		setupContext();
		assertEquals(8888, getInstanceConfig().getNonSecurePort());
	}

	@Test
	public void testDefaultInitialStatus() {
		setupContext();
		assertEquals("initialStatus wrong", InstanceStatus.UP, getInstanceConfig()
				.getInitialStatus());
	}

	@Test(expected = BeanCreationException.class)
	public void testBadInitialStatus() {
		addEnvironment(context, "eureka.instance.initial-status:FOO");
		setupContext();
	}

	@Test
	public void testCustomInitialStatus() {
		addEnvironment(context, "eureka.instance.initial-status:STARTING");
		setupContext();
		assertEquals("initialStatus wrong", InstanceStatus.STARTING, getInstanceConfig()
				.getInitialStatus());
	}

	private void setupContext() {
		context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		context.refresh();
	}

	protected EurekaInstanceConfigBean getInstanceConfig() {
		return context.getBean(EurekaInstanceConfigBean.class);
	}

	/*
	 * @Test public void serviceUrlWithCompositePropertySource() { CompositePropertySource
	 * source = new CompositePropertySource("composite");
	 * context.getEnvironment().getPropertySources().addFirst(source);
	 * source.addPropertySource(new MapPropertySource("config", Collections .<String,
	 * Object> singletonMap("eureka.client.serviceUrl.defaultZone",
	 * "http://example.com")));
	 * context.register(PropertyPlaceholderAutoConfiguration.class,
	 * TestConfiguration.class); context.refresh();
	 * assertEquals("{defaultZone=http://example.com}",
	 * context.getBean(EurekaInstanceConfigBean.class).getServiceUrl().toString());
	 * assertEquals( "[http://example.com]",
	 * context.getBean(EurekaInstanceConfigBean.class)
	 * .getEurekaServerServiceUrls("defaultZone").toString()); }
	 * 
	 * @Test public void serviceUrlWithDefault() {
	 * EnvironmentTestUtils.addEnvironment(context,
	 * "eureka.client.serviceUrl.defaultZone:",
	 * "eureka.client.serviceUrl.default:http://example.com");
	 * context.register(PropertyPlaceholderAutoConfiguration.class,
	 * TestConfiguration.class); context.refresh(); assertEquals( "[http://example.com]",
	 * context.getBean(EurekaInstanceConfigBean.class)
	 * .getEurekaServerServiceUrls("defaultZone").toString()); }
	 */
	@Configuration
	@EnableConfigurationProperties(EurekaInstanceConfigBean.class)
	protected static class TestConfiguration {

	}

}
