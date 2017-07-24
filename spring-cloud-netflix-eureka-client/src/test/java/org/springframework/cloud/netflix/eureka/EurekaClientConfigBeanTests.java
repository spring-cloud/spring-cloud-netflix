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

package org.springframework.cloud.netflix.eureka;

import java.util.Collections;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class EurekaClientConfigBeanTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void init() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void basicBinding() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"eureka.client.proxyHost=example.com");
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
		assertEquals("example.com", this.context.getBean(EurekaClientConfigBean.class)
				.getProxyHost());
	}

	@Test
	public void serviceUrl() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"eureka.client.serviceUrl.defaultZone:http://example.com");
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
		assertEquals("{defaultZone=http://example.com}",
				this.context.getBean(EurekaClientConfigBean.class).getServiceUrl()
						.toString());
		assertEquals("[http://example.com/]", getEurekaServiceUrlsForDefaultZone());
	}

	@Test
	public void serviceUrlWithCompositePropertySource() {
		CompositePropertySource source = new CompositePropertySource("composite");
		this.context.getEnvironment().getPropertySources().addFirst(source);
		source.addPropertySource(new MapPropertySource("config", Collections
				.<String, Object> singletonMap("eureka.client.serviceUrl.defaultZone",
						"http://example.com,http://example2.com")));
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
		assertEquals("{defaultZone=http://example.com,http://example2.com}",
				this.context.getBean(EurekaClientConfigBean.class).getServiceUrl()
						.toString());
		assertEquals("[http://example.com/, http://example2.com/]",
				getEurekaServiceUrlsForDefaultZone());
	}

	@Test
	public void serviceUrlWithDefault() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"eureka.client.serviceUrl.defaultZone:http://example.com");
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
		assertEquals("[http://example.com/]", getEurekaServiceUrlsForDefaultZone());
	}

	@Test
	public void serviceUrlWithCustomZone() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"eureka.client.serviceUrl.customZone:http://custom-example.com");
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
		assertEquals("[http://custom-example.com/]", getEurekaServiceUrls("customZone"));
	}

	@Test
	public void serviceUrlWithEmptyServiceUrls() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"eureka.client.serviceUrl.defaultZone:");
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
		assertEquals("[]", getEurekaServiceUrlsForDefaultZone());
	}

	private String getEurekaServiceUrlsForDefaultZone() {
		return getEurekaServiceUrls("defaultZone");
	}

	private String getEurekaServiceUrls(String myZone) {
		return this.context.getBean(EurekaClientConfigBean.class)
				.getEurekaServerServiceUrls(myZone).toString();
	}

	@Configuration
	@EnableConfigurationProperties(EurekaClientConfigBean.class)
	protected static class TestConfiguration {

	}

}
