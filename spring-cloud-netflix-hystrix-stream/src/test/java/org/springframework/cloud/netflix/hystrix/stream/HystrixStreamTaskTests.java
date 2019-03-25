/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.netflix.hystrix.stream;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesCommandDefault;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class HystrixStreamTaskTests {

	@Mock
	MessageChannel outboundChannel;

	@Mock
	DiscoveryClient discoveryClient;

	@Mock
	ApplicationContext context;

	@Spy
	HystrixStreamProperties properties;

	@Mock
	Registration registration;

	@InjectMocks
	HystrixStreamTask hystrixStreamTask;

	@Test
	public void should_not_send_metrics_when_they_are_empty() throws Exception {
		this.hystrixStreamTask.sendMetrics();

		verifyZeroInteractions(this.outboundChannel);
	}

	@Test
	public void should_send_metrics_when_they_are_not_empty() throws Exception {
		this.hystrixStreamTask.jsonMetrics.put("someJson");

		this.hystrixStreamTask.sendMetrics();

		then(this.outboundChannel).should().send(any(Message.class));
	}

	@Test
	public void should_gather_json_metrics() throws Exception {
		HystrixCommandKey hystrixCommandKey = HystrixCommandKey.Factory
				.asKey("commandKey");
		HystrixCommandMetrics.getInstance(hystrixCommandKey,
				HystrixCommandGroupKey.Factory.asKey("commandGroupKey"),
				new HystrixPropertiesCommandDefault(hystrixCommandKey,
						HystrixCommandProperties.defaultSetter()));

		this.hystrixStreamTask.setApplicationContext(this.context);
		this.hystrixStreamTask.gatherMetrics();

		assertThat(this.hystrixStreamTask.jsonMetrics.isEmpty()).isFalse();
	}

}
