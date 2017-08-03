/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.netflix.feign.valid;

import org.junit.Test;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignAutoConfiguration;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.feign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 */
public class FeignClientValidationTests {

	@Test
	public void validNotLoadBalanced() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				GoodUrlConfiguration.class);
		assertNotNull(context.getBean(GoodUrlConfiguration.Client.class));
		context.close();
	}

	@Configuration
	@Import({FeignAutoConfiguration.class, HttpClientConfiguration.class})
	@EnableFeignClients(clients = GoodUrlConfiguration.Client.class)
	protected static class GoodUrlConfiguration {

		@FeignClient(name="example", url="http://example.com")
		interface Client {
			@RequestMapping(method = RequestMethod.GET, value = "/")
			@Deprecated
			String get();
		}

	}

	@Test
	public void validPlaceholder() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				PlaceholderUrlConfiguration.class);
		assertNotNull(context.getBean(PlaceholderUrlConfiguration.Client.class));
		context.close();
	}

	@Configuration
	@Import({FeignAutoConfiguration.class, HttpClientConfiguration.class})
	@EnableFeignClients(clients = PlaceholderUrlConfiguration.Client.class)
	protected static class PlaceholderUrlConfiguration {

		@FeignClient(name="example", url="${feignClient.url:http://example.com}")
		interface Client {
			@RequestMapping(method = RequestMethod.GET, value = "/")
			@Deprecated
			String get();
		}

	}

	@Test
	public void validLoadBalanced() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				LoadBalancerAutoConfiguration.class,
				RibbonAutoConfiguration.class,
				FeignRibbonClientAutoConfiguration.class,
				GoodServiceIdConfiguration.class);
		assertNotNull(context.getBean(GoodServiceIdConfiguration.Client.class));
		context.close();
	}

	@Configuration
	@Import({FeignAutoConfiguration.class, HttpClientConfiguration.class})
	@EnableFeignClients(clients = GoodServiceIdConfiguration.Client.class)
	protected static class GoodServiceIdConfiguration {

		@FeignClient("foo")
		interface Client {
			@RequestMapping(method = RequestMethod.GET, value = "/")
			@Deprecated
			String get();
		}

	}

}
