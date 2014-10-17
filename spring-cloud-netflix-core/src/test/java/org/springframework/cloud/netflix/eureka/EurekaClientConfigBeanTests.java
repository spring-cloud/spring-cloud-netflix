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

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;

/**
 * @author Dave Syer
 *
 */
public class EurekaClientConfigBeanTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void init() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void basicBinding() {
		EnvironmentTestUtils.addEnvironment(context,
				"eureka.client.proxyHost=example.com");
		context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		context.refresh();
		assertEquals("example.com", context.getBean(EurekaClientConfigBean.class)
				.getProxyHost());
	}

	@Test
	public void serviceUrl() {
		EnvironmentTestUtils.addEnvironment(context,
				"eureka.client.serviceUrl.defaultZone:http://example.com");
		context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		context.refresh();
		assertEquals("{defaultZone=http://example.com}",
				context.getBean(EurekaClientConfigBean.class).getServiceUrl().toString());
		assertEquals(
				"[http://example.com]",
				context.getBean(EurekaClientConfigBean.class)
						.getEurekaServerServiceUrls("defaultZone").toString());
	}

	@Test
	public void serviceUrlWithCompositePropertySource() {
		CompositePropertySource source = new CompositePropertySource("composite");
		context.getEnvironment().getPropertySources().addFirst(source);
		source.addPropertySource(new MapPropertySource("config", Collections
				.<String, Object> singletonMap("eureka.client.serviceUrl.defaultZone",
						"http://example.com")));
		context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		context.refresh();
		assertEquals("{defaultZone=http://example.com}",
				context.getBean(EurekaClientConfigBean.class).getServiceUrl().toString());
		assertEquals(
				"[http://example.com]",
				context.getBean(EurekaClientConfigBean.class)
						.getEurekaServerServiceUrls("defaultZone").toString());
	}

	@Test
	public void serviceUrlWithDefault() {
		EnvironmentTestUtils.addEnvironment(context,
				"eureka.client.serviceUrl.defaultZone:http://example.com");
		context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		context.refresh();
		assertEquals(
				"[http://example.com]",
				context.getBean(EurekaClientConfigBean.class)
						.getEurekaServerServiceUrls("defaultZone").toString());
	}

	@Configuration
	@EnableConfigurationProperties(EurekaClientConfigBean.class)
	protected static class TestConfiguration {

	}

}
