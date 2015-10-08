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

package org.springframework.cloud.netflix.feign.ribbon;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;

/**
 * @author Spencer Gibb
 */
public class CachingSpringLoadBalancerFactoryTests {

	@Mock
	private SpringClientFactory delegate;

	private CachingSpringLoadBalancerFactory factory;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);

		IClientConfig config = new DefaultClientConfigImpl();
		config.set(CommonClientConfigKey.ConnectTimeout, 1000);
		config.set(CommonClientConfigKey.ReadTimeout, 500);

		when(this.delegate.getClientConfig("client1")).thenReturn(config);
		when(this.delegate.getClientConfig("client2")).thenReturn(config);

		this.factory = new CachingSpringLoadBalancerFactory(this.delegate);
	}

	@Test
	public void delegateCreatesWhenMissing() {
		FeignLoadBalancer client = this.factory.create("client1");
		assertNotNull("client was null", client);

		verify(this.delegate, times(1)).getClientConfig("client1");
	}

	@Test
	public void cacheWorks() {
		FeignLoadBalancer client = this.factory.create("client2");
		assertNotNull("client was null", client);

		client = this.factory.create("client2");
		assertNotNull("client was null", client);

		verify(this.delegate, times(1)).getClientConfig("client2");
	}

}
