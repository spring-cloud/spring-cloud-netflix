/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.cloud.netflix.ribbon.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.netflix.ribbon.support.SpringAbstractLoadBalancerAwareClient;
import org.springframework.cloud.netflix.ribbon.support.SpringRestClient;

import com.netflix.client.AbstractLoadBalancerAwareClient;
import com.netflix.client.ClientFactory;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpResponse;
import com.netflix.config.ConfigurationManager;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.Server;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * @author Jin Zhang
 */
public class LoadBalancerKeyTest {
	private HttpServer httpServer;
	private String host;
	private int port;

	@Before
	public void before() throws Exception {
		InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", 0);
		httpServer = HttpServer.create(inetSocketAddress, 0);
		httpServer.createContext("/", new HttpHandler() {
			@Override
			public void handle(HttpExchange httpExchange) throws IOException {
				httpExchange.sendResponseHeaders(200, 0);
				httpExchange.close();
			}
		});
		host = httpServer.getAddress().getHostName();
		port = httpServer.getAddress().getPort();
		httpServer.start();
	}

	@After
	public void after() {
		httpServer.stop(0);
	}

	@Test
	public void testSpringRestClient() throws Exception {
		String service = "test0";
		ConfigurationManager.getConfigInstance().setProperty(service + ".ribbon." + CommonClientConfigKey.ClientClassName,
				SpringRestClient.class.getName());
		SpringRestClient client = (SpringRestClient) ClientFactory.getNamedClient(service);
		testExecuteWithLB(client);
	}

	@Test
	public void testSpringAbstractLoadBalancerAwareClient() throws Exception {
		String service = "test1";
		ConfigurationManager.getConfigInstance().setProperty(service + ".ribbon." + CommonClientConfigKey.ClientClassName,
				TestSpringAbstractLoadBalancerAwareClient.class.getName());
		TestSpringAbstractLoadBalancerAwareClient client =
				(TestSpringAbstractLoadBalancerAwareClient) ClientFactory.getNamedClient(service);
		testExecuteWithLB(client);
	}

	public void testExecuteWithLB(AbstractLoadBalancerAwareClient<HttpRequest, HttpResponse> client) throws Exception {

		BaseLoadBalancer lb = new BaseLoadBalancer();
		Server[] servers = new Server[]{new Server(host, port)};
		lb.addServers(Arrays.asList(servers));

		client.setLoadBalancer(lb);

		Object[] lbKeys = new Object[]{"test", 1, null};
		for (Object lbKey: lbKeys) {
			lb.setRule(createRule(lbKey));
			HttpRequest request = createRequest(lbKey);
			HttpResponse response = client.executeWithLoadBalancer(request);
			String content = response.getEntity(String.class);
			response.close();

			Assert.assertTrue(content.isEmpty());
			Assert.assertEquals(serverURI(), response.getRequestedURI());
			Assert.assertEquals(200, response.getStatus());
		}
	}

	static abstract class TestLbKeyRule implements IRule {
		private ILoadBalancer lb;

		public abstract void test(Object key);

		@Override
		public Server choose(Object key) {
			test(key);
			return getLoadBalancer().getAllServers().get(0);
		}

		@Override
		public void setLoadBalancer(ILoadBalancer lb) {
			this.lb = lb;
		}

		@Override
		public ILoadBalancer getLoadBalancer() {
			return this.lb;
		}
	}

	private TestLbKeyRule createRule(final Object expectedKey) {
		return new TestLbKeyRule() {
			@Override
			public void test(Object key) {
				Assert.assertEquals(expectedKey, key);
			}
		};
	}

	private HttpRequest createRequest(Object lbKey) {
		return HttpRequest.newBuilder()
				.uri("/")
				.loadBalancerKey(lbKey)
				.build();
	}

	private URI serverURI() throws Exception {
		return new URI("http://" + host + ":" + port + "/");
	}

	public static class TestSpringAbstractLoadBalancerAwareClient extends SpringAbstractLoadBalancerAwareClient<HttpRequest, HttpResponse> {

		public TestSpringAbstractLoadBalancerAwareClient() {
			super(null);
		}

		@Override
		public RequestSpecificRetryHandler getRequestSpecificRetryHandler(HttpRequest request, IClientConfig requestConfig) {
			return null;
		}

		@Override
		public HttpResponse execute(HttpRequest request, IClientConfig requestConfig) throws Exception {
			HttpResponse response = Mockito.mock(HttpResponse.class);
			Mockito.when(response.getEntity(String.class)).thenReturn("");
			Mockito.when(response.getRequestedURI()).thenReturn(request.getUri());
			Mockito.when(response.getStatus()).thenReturn(200);
			return response;
		}
	}

}
