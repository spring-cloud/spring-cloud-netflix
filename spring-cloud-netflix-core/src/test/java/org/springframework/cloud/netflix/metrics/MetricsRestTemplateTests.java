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

package org.springframework.cloud.netflix.metrics;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.netflix.ribbon.RibbonClientHttpRequestFactory;
import org.springframework.cloud.netflix.ribbon.RibbonClientHttpRequestFactoryTests;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertTrue;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = MetricsRestTemplateTests.App.class)
@WebIntegrationTest(value = { "spring.application.name=ribbonclienttest",
		"spring.jmx.enabled=true" }, randomPort = true)
@DirtiesContext
public class MetricsRestTemplateTests extends RibbonClientHttpRequestFactoryTests {

	@Test
	@Override
	public void requestFactoryIsRibbon() {
		ClientHttpRequestFactory requestFactory = this.restTemplate.getRequestFactory();
		assertTrue("wrong RequestFactory type: " + requestFactory.getClass(),
				requestFactory instanceof InterceptingClientHttpRequestFactory);

		InterceptingClientHttpRequestFactory intercepting = (InterceptingClientHttpRequestFactory) requestFactory;

		Object realRequestFactory = ReflectionTestUtils.getField(intercepting,
				"requestFactory");
		assertTrue("wrong RequestFactory type: " + realRequestFactory.getClass(),
				realRequestFactory instanceof RibbonClientHttpRequestFactory);
	}

}
