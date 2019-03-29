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

import java.util.Collections;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

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
		TestPropertyValues.of("eureka.client.proxyHost=example.com")
				.applyTo(this.context);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(EurekaClientConfigBean.class).getProxyHost())
				.isEqualTo("example.com");
	}

	@Test
	public void serviceUrl() {
		TestPropertyValues.of("eureka.client.serviceUrl.defaultZone:https://example.com")
				.applyTo(this.context);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(EurekaClientConfigBean.class).getServiceUrl()
				.toString()).isEqualTo("{defaultZone=https://example.com}");
		assertThat(getEurekaServiceUrlsForDefaultZone())
				.isEqualTo("[https://example.com/]");
	}

	@Test
	public void serviceUrlWithCompositePropertySource() {
		CompositePropertySource source = new CompositePropertySource("composite");
		this.context.getEnvironment().getPropertySources().addFirst(source);
		source.addPropertySource(new MapPropertySource("config",
				Collections.<String, Object>singletonMap(
						"eureka.client.serviceUrl.defaultZone",
						"https://example.com,https://example2.com, https://www.hugedomains.com/domain_profile.cfm?d=example3&e=com")));
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(EurekaClientConfigBean.class).getServiceUrl()
				.toString()).isEqualTo(
						"{defaultZone=https://example.com,https://example2.com, https://www.hugedomains.com/domain_profile.cfm?d=example3&e=com}");
		assertThat(getEurekaServiceUrlsForDefaultZone()).isEqualTo(
				"[https://example.com/, https://example2.com/, https://www.hugedomains.com/domain_profile.cfm?d=example3&e=com/]");
	}

	@Test
	public void serviceUrlWithDefault() {
		TestPropertyValues.of("eureka.client.serviceUrl.defaultZone:https://example.com")
				.applyTo(this.context);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
		assertThat(getEurekaServiceUrlsForDefaultZone())
				.isEqualTo("[https://example.com/]");
	}

	@Test
	public void serviceUrlWithCustomZone() {
		TestPropertyValues
				.of("eureka.client.serviceUrl.customZone:https://custom-example.com")
				.applyTo(this.context);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
		assertThat(getEurekaServiceUrls("customZone"))
				.isEqualTo("[https://custom-example.com/]");
	}

	@Test
	public void serviceUrlWithEmptyServiceUrls() {
		TestPropertyValues.of("eureka.client.serviceUrl.defaultZone:")
				.applyTo(this.context);
		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				TestConfiguration.class);
		this.context.refresh();
		assertThat(getEurekaServiceUrlsForDefaultZone()).isEqualTo("[]");
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
