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

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.client.loadbalancer.LoadBalancedBackOffPolicyFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryListenerFactory;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Spencer Gibb
 */
public class CachingSpringLoadBalancerFactoryTests {

	@Mock
	private SpringClientFactory delegate;

	@Mock
	private RibbonLoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory;

	@Mock
	private LoadBalancedBackOffPolicyFactory loadBalancedBackOffPolicyFactory;

	@Mock
	private LoadBalancedRetryListenerFactory loadBalancedRetryListenerFactory;

	private CachingSpringLoadBalancerFactory factory;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);

		IClientConfig config = new DefaultClientConfigImpl();
		config.set(CommonClientConfigKey.ConnectTimeout, 1000);
		config.set(CommonClientConfigKey.ReadTimeout, 500);

		when(this.delegate.getClientConfig("client1")).thenReturn(config);
		when(this.delegate.getClientConfig("client2")).thenReturn(config);

		this.factory = new CachingSpringLoadBalancerFactory(this.delegate,
				loadBalancedRetryPolicyFactory);
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

	@Test
	public void delegateCreatesWithNoRetry() {
		IClientConfig config = new DefaultClientConfigImpl();
		config.set(CommonClientConfigKey.ConnectTimeout, 1000);
		config.set(CommonClientConfigKey.ReadTimeout, 500);
		when(this.delegate.getClientConfig("retry")).thenReturn(config);
		CachingSpringLoadBalancerFactory factory = new CachingSpringLoadBalancerFactory(this.delegate);
		FeignLoadBalancer client = this.factory.create("retry");
		assertNotNull("client was null", client);
	}

	@Test
	public void delegateCreatesWithRetry() {
		IClientConfig config = new DefaultClientConfigImpl();
		config.set(CommonClientConfigKey.ConnectTimeout, 1000);
		config.set(CommonClientConfigKey.ReadTimeout, 500);
		when(this.delegate.getClientConfig("retry")).thenReturn(config);
		CachingSpringLoadBalancerFactory factory = new CachingSpringLoadBalancerFactory(
			this.delegate, loadBalancedRetryPolicyFactory, false);
		FeignLoadBalancer client = this.factory.create("retry");
		assertNotNull("client was null", client);
	}

	@Test
	public void delegateCreatesWithBackOff() {
		IClientConfig config = new DefaultClientConfigImpl();
		config.set(CommonClientConfigKey.ConnectTimeout, 1000);
		config.set(CommonClientConfigKey.ReadTimeout, 500);
		when(this.delegate.getClientConfig("retry")).thenReturn(config);
		CachingSpringLoadBalancerFactory factory = new CachingSpringLoadBalancerFactory(
			this.delegate, loadBalancedRetryPolicyFactory, loadBalancedBackOffPolicyFactory);
		FeignLoadBalancer client = this.factory.create("retry");
		assertNotNull("client was null", client);
	}

	@Test
	public void delegateCreatesWithRetryListener() {
		IClientConfig config = new DefaultClientConfigImpl();
		config.set(CommonClientConfigKey.ConnectTimeout, 1000);
		config.set(CommonClientConfigKey.ReadTimeout, 500);
		when(this.delegate.getClientConfig("retry")).thenReturn(config);
		CachingSpringLoadBalancerFactory factory = new CachingSpringLoadBalancerFactory(
			this.delegate, loadBalancedRetryPolicyFactory, loadBalancedBackOffPolicyFactory, loadBalancedRetryListenerFactory);
		FeignLoadBalancer client = this.factory.create("retry");
		assertNotNull("client was null", client);
	}

}
