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
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 */
public class FeignClientValidationTests {

	@Test
	public void valid() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				GoodConfiguration.class);
		assertNotNull(context.getBean(GoodConfiguration.Client.class));
		context.close();
	}

	@Configuration
	@EnableFeignClients
	protected static class GoodConfiguration {

		@FeignClient("foo")
		interface Client {
			@RequestMapping(method = RequestMethod.GET, value = "/")
			String get();
		}

	}

}
