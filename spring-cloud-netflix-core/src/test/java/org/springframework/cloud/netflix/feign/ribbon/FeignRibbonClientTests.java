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

import org.hamcrest.CustomMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.RequestTemplate;

/**
 * @author Dave Syer
 */
public class FeignRibbonClientTests {

	private ILoadBalancer loadBalancer = Mockito.mock(ILoadBalancer.class);
	private Client delegate = Mockito.mock(Client.class);

	private SpringClientFactory factory = new SpringClientFactory() {
		@Override
		public IClientConfig getClientConfig(String name) {
			DefaultClientConfigImpl config = new DefaultClientConfigImpl();
			config.set(CommonClientConfigKey.ConnectTimeout, 1000);
			config.set(CommonClientConfigKey.ReadTimeout, 500);
			return config;
		}

		@Override
		public ILoadBalancer getLoadBalancer(String name) {
			return FeignRibbonClientTests.this.loadBalancer;
		}
	};

	private FeignRibbonClient client = new FeignRibbonClient(this.factory);

	@Before
	public void init() {
		ReflectionTestUtils.setField(this.client, "defaultClient", this.delegate);
		Mockito.when(this.loadBalancer.chooseServer(Matchers.any())).thenReturn(
				new Server("foo.com", 8000));
	}

	@Test
	public void remoteRequestIsSent() throws Exception {
		Request request = new RequestTemplate().method("GET").append("http://foo/")
				.request();
		this.client.execute(request, new Options());
		RequestMatcher matcher = new RequestMatcher("http://foo.com:8000/");
		Mockito.verify(this.delegate).execute(Matchers.argThat(matcher),
				Matchers.any(Options.class));
	}

	@Test
	public void remoteRequestIsSecure() throws Exception {
		Request request = new RequestTemplate().method("GET").append("https://foo/")
				.request();
		this.client.execute(request, new Options());
		RequestMatcher matcher = new RequestMatcher("https://foo.com:8000/");
		Mockito.verify(this.delegate).execute(Matchers.argThat(matcher),
				Matchers.any(Options.class));
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
