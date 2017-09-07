/*
 * Copyright 2013-2017 the original author or authors.
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
 *
 */

package org.springframework.cloud.netflix.feign.ribbon;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.netflix.feign.ribbon.FeignLoadBalancer.RibbonRequest;
import org.springframework.cloud.netflix.feign.ribbon.FeignLoadBalancer.RibbonResponse;
import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import feign.Client;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.Request.Options;

import static com.netflix.client.config.CommonClientConfigKey.ConnectTimeout;
import static com.netflix.client.config.CommonClientConfigKey.IsSecure;
import static com.netflix.client.config.CommonClientConfigKey.MaxAutoRetries;
import static com.netflix.client.config.CommonClientConfigKey.MaxAutoRetriesNextServer;
import static com.netflix.client.config.CommonClientConfigKey.OkToRetryOnAllOperations;
import static com.netflix.client.config.CommonClientConfigKey.ReadTimeout;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class FeignLoadBalancerTests {

	@Mock
	private Client delegate;
	@Mock
	private ILoadBalancer lb;
	@Mock
	private IClientConfig config;

	private FeignLoadBalancer feignLoadBalancer;

	private ServerIntrospector inspector = new DefaultServerIntrospector();

	private Integer defaultConnectTimeout = 10000;
	private Integer defaultReadTimeout = 10000;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		when(this.config.get(MaxAutoRetries, DEFAULT_MAX_AUTO_RETRIES)).thenReturn(1);
		when(this.config.get(MaxAutoRetriesNextServer,
				DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER)).thenReturn(1);
		when(this.config.get(OkToRetryOnAllOperations, eq(anyBoolean())))
				.thenReturn(true);
		when(this.config.get(ConnectTimeout)).thenReturn(this.defaultConnectTimeout);
		when(this.config.get(ReadTimeout)).thenReturn(this.defaultReadTimeout);
		when(this.config.get(OkToRetryOnAllOperations, false)).thenReturn(true);
	}

	@Test
	public void testUriInsecure() throws Exception {
		when(this.config.get(IsSecure)).thenReturn(false);

		this.feignLoadBalancer = new FeignLoadBalancer(this.lb, this.config,
				this.inspector);
		Request request = new RequestTemplate().method("GET").append("http://foo/")
				.request();
		RibbonRequest ribbonRequest = new RibbonRequest(this.delegate, request,
				new URI(request.url()));

		Response response = Response.create(200, "Test",
				Collections.<String, Collection<String>> emptyMap(), new byte[0]);
		when(this.delegate.execute(any(Request.class), any(Options.class)))
				.thenReturn(response);

		RibbonResponse resp = this.feignLoadBalancer.execute(ribbonRequest, null);

		assertThat(resp.getRequestedURI(), is(new URI("http://foo/")));
	}

	@Test
	public void testSecureUriFromClientConfig() throws Exception {
		when(this.config.get(IsSecure)).thenReturn(true);
		this.feignLoadBalancer = new FeignLoadBalancer(this.lb, this.config,
				this.inspector);
		Server server = new Server("foo", 7777);
		URI uri = this.feignLoadBalancer.reconstructURIWithServer(server,
				new URI("http://foo/"));
		assertThat(uri, is(new URI("https://foo:7777/")));
	}

	@Test
	public void testInsecureUriFromInsecureClientConfigToSecureServerIntrospector()
			throws Exception {
		when(this.config.get(IsSecure)).thenReturn(false);
		this.feignLoadBalancer = new FeignLoadBalancer(this.lb, this.config,
				new ServerIntrospector() {
					@Override
					public boolean isSecure(Server server) {
						return true;
					}

					@Override
					public Map<String, String> getMetadata(Server server) {
						return null;
					}
				});
		Server server = new Server("foo", 7777);
		URI uri = this.feignLoadBalancer.reconstructURIWithServer(server,
				new URI("http://foo/"));
		assertThat(uri, is(new URI("http://foo:7777/")));
	}

	@Test
	public void testSecureUriFromClientConfigOverride() throws Exception {
		this.feignLoadBalancer = new FeignLoadBalancer(this.lb, this.config,
				this.inspector);
		Server server = Mockito.mock(Server.class);
		when(server.getPort()).thenReturn(443);
		when(server.getHost()).thenReturn("foo");
		URI uri = this.feignLoadBalancer.reconstructURIWithServer(server,
				new URI("http://bar/"));
		assertThat(uri, is(new URI("https://foo:443/")));
	}

	@Test
	public void testRibbonRequestURLEncode() throws Exception {
		String url = "http://foo/?name=%7bcookie";//name={cookie
		Request request = Request.create("GET",url,new HashMap(),null,null);

		assertThat(request.url(),is(url));

		RibbonRequest ribbonRequest = new RibbonRequest(this.delegate,request,new URI(request.url()));

		Request cloneRequest = ribbonRequest.toRequest();

		assertThat(cloneRequest.url(),is(url));

	}

}
