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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.ribbon.RibbonClientHttpRequestFactory;
import org.springframework.cloud.netflix.ribbon.RibbonClientHttpRequestFactoryTests;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MetricsRestTemplateTests.App.class, webEnvironment = WebEnvironment.RANDOM_PORT, value = {
		"spring.application.name=ribbonclienttest", "spring.jmx.enabled=false",
		"ribbon.http.client.enabled=true" })
@DirtiesContext
public class MetricsRestTemplateTests extends RibbonClientHttpRequestFactoryTests {

	@Test
	@Override
	public void requestFactoryIsRibbon() {
		ClientHttpRequestFactory requestFactory = this.restTemplate.getRequestFactory();
		assertThat("wrong RequestFactory type: " + requestFactory.getClass(),
				requestFactory,
				is(instanceOf(InterceptingClientHttpRequestFactory.class)));

		InterceptingClientHttpRequestFactory intercepting = (InterceptingClientHttpRequestFactory) requestFactory;
		Object interceptorsField = ReflectionTestUtils.getField(intercepting,
				"interceptors");
		assertThat("wrong interceptors type: " + interceptorsField.getClass(),
				interceptorsField, is(instanceOf(List.class)));
		@SuppressWarnings("unchecked")
		List<ClientHttpRequestInterceptor> interceptors = (List<ClientHttpRequestInterceptor>) interceptorsField;
		assertThat("interceptors is wrong size", interceptors, hasSize(1));
		assertThat("wrong interceptor type", interceptors.get(0),
				is(instanceOf(MetricsClientHttpRequestInterceptor.class)));

		Object realRequestFactory = ReflectionTestUtils.getField(intercepting,
				"requestFactory");
		assertThat("wrong RequestFactory type: " + realRequestFactory.getClass(),
				realRequestFactory, is(instanceOf(RibbonClientHttpRequestFactory.class)));
	}

}
