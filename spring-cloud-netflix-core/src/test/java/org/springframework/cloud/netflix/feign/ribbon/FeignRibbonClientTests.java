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
import com.netflix.loadbalancer.AbstractLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.LoadBalancerStats;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerStats;
import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.RequestTemplate;
import org.hamcrest.CustomMatcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dave Syer
 * @author Spencer Gibb
 */
public class FeignRibbonClientTests {

	private AbstractLoadBalancer loadBalancer = mock(AbstractLoadBalancer.class);
	private Client delegate = mock(Client.class);
	private RibbonLoadBalancedRetryPolicyFactory retryPolicyFactory = mock(RibbonLoadBalancedRetryPolicyFactory.class);

	private SpringClientFactory factory = new SpringClientFactory() {
		@Override
		public IClientConfig getClientConfig(String name) {
			DefaultClientConfigImpl config = new DefaultClientConfigImpl();
			config.set(CommonClientConfigKey.ConnectTimeout, 1000);
			config.set(CommonClientConfigKey.ReadTimeout, 500);
			return config;
		}

		@Override
		public <C> C getInstance(String name, Class<C> type) {
			if (type.isAssignableFrom(ServerIntrospector.class)) {
				@SuppressWarnings("unchecked")
				C instance = (C) new DefaultServerIntrospector();
				return instance;
			}
			return null;
		}

		@Override
		public ILoadBalancer getLoadBalancer(String name) {
			return FeignRibbonClientTests.this.loadBalancer;
		}
	};

	// Even though we don't maintain FeignRibbonClient, keep these tests
	// around to make sure the expected behaviour doesn't break
	private Client client = new LoadBalancerFeignClient(this.delegate, new CachingSpringLoadBalancerFactory(this.factory,
			retryPolicyFactory), this.factory);

	@Before
	public void init() {
		when(this.loadBalancer.chooseServer(any())).thenReturn(
				new Server("foo.com", 8000));
		//to fix NPE
		LoadBalancerStats stats = mock(LoadBalancerStats.class);
		when(this.loadBalancer.getLoadBalancerStats()).thenReturn(stats);
		when(stats.getSingleServerStat(any(Server.class))).thenReturn(mock(ServerStats.class));
	}

	@Test
	public void remoteRequestIsSent() throws Exception {
		Request request = new RequestTemplate().method("GET").append("http://foo/")
				.request();
		this.client.execute(request, new Options());
		RequestMatcher matcher = new RequestMatcher("http://foo.com:8000/");
		verify(this.delegate).execute(argThat(matcher),
				any(Options.class));
	}

	@Test
	public void remoteRequestIsSecure() throws Exception {
		Request request = new RequestTemplate().method("GET").append("https://foo/")
				.request();
		this.client.execute(request, new Options());
		RequestMatcher matcher = new RequestMatcher("https://foo.com:8000/");
		verify(this.delegate).execute(argThat(matcher),
				any(Options.class));
	}

	private final static class RequestMatcher extends CustomMatcher<Request> {
		private String url;

		private RequestMatcher(String url) {
			super("request has URI: " + url);
			this.url = url;
		}

		@Override
		public boolean matches(Object item) {
			Request request = (Request) item;
			return request.url().equals(this.url);
		}
	}

}
