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

package org.springframework.cloud.netflix.feign.invalid;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignAutoConfiguration;
import org.springframework.cloud.netflix.feign.FeignClient;
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

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Test
	public void invalid() {
		this.expected.expectMessage("not legal hostname (foo_bar)");
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BadConfiguration.class);
		assertNotNull(context.getBean(BadConfiguration.Client.class));
		context.close();
	}

	@Configuration
	@Import(FeignAutoConfiguration.class)
	@EnableFeignClients(clients = BadConfiguration.Client.class)
	protected static class BadConfiguration {

		@FeignClient("foo_bar")
		interface Client {
			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();
		}

	}

}
