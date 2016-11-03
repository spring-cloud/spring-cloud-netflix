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

package org.springframework.cloud.netflix.ribbon;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient.RibbonServer;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.util.UriComponentsBuilder;
import com.netflix.loadbalancer.Server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;

/**
 * @author Spencer Gibb
 */
public class RibbonInterceptorTests {

	@Mock
	private HttpRequest request;

	@Mock
	private ClientHttpRequestExecution execution;

	@Mock
	private ClientHttpResponse response;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testIntercept() throws Exception {
		RibbonServer server = new RibbonServer("myservice", new Server("myhost", 8080));
		LoadBalancerInterceptor interceptor = new LoadBalancerInterceptor(new MyClient(server));
		given(this.request.getURI()).willReturn(new URL("http://myservice").toURI());
		given(this.execution.execute(isA(HttpRequest.class), isA(byte[].class)))
				.willReturn(this.response);
		ArgumentCaptor<HttpRequestWrapper> argument = ArgumentCaptor
				.forClass(HttpRequestWrapper.class);
		ClientHttpResponse response = interceptor.intercept(this.request, new byte[0],
				this.execution);
		assertNotNull("response was null", response);
		verify(this.execution).execute(argument.capture(), isA(byte[].class));
		HttpRequestWrapper wrapper = argument.getValue();
		assertEquals("wrong constructed uri", new URL("http://myhost:8080").toURI(),
				wrapper.getURI());
	}

	protected static class MyClient implements LoadBalancerClient {

		private ServiceInstance instance;

		public MyClient(ServiceInstance instance) {
			this.instance = instance;
		}

		@Override
		public ServiceInstance choose(String serviceId) {
			return this.instance;
		}

		@Override
		public <T> T execute(String serviceId, LoadBalancerRequest<T> request) {
			try {
				return request.apply(this.instance);
			}
			catch (Exception ex) {
				ReflectionUtils.rethrowRuntimeException(ex);
			}
			return null;
		}

		@Override
		public <T> T execute(String s, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) throws IOException {
			try {
				return request.apply(this.instance);
			}
			catch (Exception ex) {
				ReflectionUtils.rethrowRuntimeException(ex);
			}
			return null;
		}

		@Override
		public URI reconstructURI(ServiceInstance instance, URI original) {
			return UriComponentsBuilder.fromUri(original).host(instance.getHost())
					.port(instance.getPort()).build().toUri();
		}

	}

}
