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
 *
 */

package org.springframework.cloud.netflix.zuul;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.filters.route.RestClientRibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.okhttp.OkHttpRibbonCommandFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @author Spencer Gibb
 * @author Biju Kunjummen
 */
public class ZuulProxyConfigurationTests {

	@Test
	public void testDefaultsToApacheHttpClient() {
		testClient(HttpClientRibbonCommandFactory.class, null);
		testClient(HttpClientRibbonCommandFactory.class, "ribbon.httpclient.enabled=true");
	}

	@Test
	public void testEnableRestClient() {
		testClient(RestClientRibbonCommandFactory.class, "ribbon.restclient.enabled=true");
	}

	@Test
	public void testEnableOkHttpClient() {
		testClient(OkHttpRibbonCommandFactory.class, "ribbon.okhttp.enabled=true");
	}

	void testClient(Class<?> clientType, String property) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class, ZuulProxyMarkerConfiguration.class, 
			ZuulProxyAutoConfiguration.class);
		if (property != null) {
			EnvironmentTestUtils.addEnvironment(context, property);
		}
		context.refresh();
		RibbonCommandFactory factory = context.getBean(RibbonCommandFactory.class);
		assertThat("RibbonCommandFactory is wrong type for property: " + property, factory, is(instanceOf(clientType)));
		context.close();
	}

	static class TestConfig {
		@Bean
		ServerProperties serverProperties() {
			return new ServerProperties();
		}

		@Bean
		SpringClientFactory springClientFactory() {
			return mock(SpringClientFactory.class);
		}

		@Bean
		DiscoveryClient discoveryClient() {
			return mock(DiscoveryClient.class);
		}
	}

}
