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
import java.util.Collections;
import java.util.Map;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.LoadBalancerStats;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerStats;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient.RibbonServer;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Spencer Gibb
 * @author Tim Ysewyn
 */
public class RibbonLoadBalancerClientTests {

	@Mock
	private SpringClientFactory clientFactory;

	@Mock
	private BaseLoadBalancer loadBalancer;

	@Mock
	private LoadBalancerStats loadBalancerStats;

	@Mock
	private ServerStats serverStats;

	private Server server = null;

	@Before
	public void init() {
		server = null;
		MockitoAnnotations.initMocks(this);
		given(this.clientFactory.getLoadBalancerContext(anyString())).willReturn(
				new RibbonLoadBalancerContext(this.loadBalancer));
		given(this.clientFactory.getInstance(anyString(), eq(ServerIntrospector.class)))
				.willReturn(new DefaultServerIntrospector() {

					@Override
					public boolean isSecure(Server server) {
						RibbonLoadBalancerClientTests.this.server = server;
						return super.isSecure(server);
					}

					@Override
					public Map<String, String> getMetadata(Server server) {
						return Collections.singletonMap("mykey", "myvalue");
					}
				});
	}

	@Test
	public void reconstructURI() throws Exception {
		testReconstructURI("http");
	}

	@Test
	public void reconstructSecureURI() throws Exception {
		testReconstructURI("https");
	}

	private void testReconstructURI(String scheme) throws Exception {
		RibbonServer server = getRibbonServer();
		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
		ServiceInstance serviceInstance = client.choose(server.getServiceId());
		URI uri = client.reconstructURI(serviceInstance,
				new URL(scheme + "://" + server.getServiceId()).toURI());
		assertThat(uri).hasScheme(scheme)
				.hasHost(serviceInstance.getHost())
				.hasPort(serviceInstance.getPort());
		assertThat(this.server).isNotNull()
				.isInstanceOf(MyServer.class);
	}

	@Test
	public void testReconstructSecureUriWithSpecialCharsPath() {
		testReconstructUriWithPath("https", "/foo=|");
	}

	@Test
	public void testReconstructUnsecureUriWithSpecialCharsPath() {
		testReconstructUriWithPath("http", "/foo=|");
	}

	private void testReconstructUriWithPath(String scheme, String path) {
		RibbonServer server = getRibbonServer();
		IClientConfig config = mock(IClientConfig.class);
		when(config.get(CommonClientConfigKey.IsSecure)).thenReturn(true);
		when(clientFactory.getClientConfig(server.getServiceId())).thenReturn(config);

		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
		ServiceInstance serviceInstance = client.choose(server.getServiceId());

		URI expanded = new DefaultUriBuilderFactory()
				.expand(scheme + "://" + server.getServiceId() + path);
		URI reconstructed = client.reconstructURI(serviceInstance, expanded);
		assertThat(reconstructed).hasPath(path);
	}

	@Test
	public void testReconstructHonorsRibbonServerScheme() {
		RibbonServer server = new RibbonServer("testService",
				new Server("ws", "myhost", 9080), false,
				Collections.singletonMap("mykey", "myvalue"));

		IClientConfig config = mock(IClientConfig.class);
		when(config.get(CommonClientConfigKey.IsSecure)).thenReturn(false);
		when(clientFactory.getClientConfig(server.getServiceId())).thenReturn(config);

		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
		ServiceInstance serviceInstance = client.choose(server.getServiceId());
		URI uri = client.reconstructURI(serviceInstance, URI.create("http://testService"));

		assertThat(uri).hasScheme("ws").hasHost("myhost").hasPort(9080);
	}

	@Test
	public void testReconstructUriWithSecureClientConfig() throws Exception {
		RibbonServer server = getRibbonServer();
		IClientConfig config = mock(IClientConfig.class);
		when(config.get(CommonClientConfigKey.IsSecure)).thenReturn(true);
		when(clientFactory.getClientConfig(server.getServiceId())).thenReturn(config);

		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
		ServiceInstance serviceInstance = client.choose(server.getServiceId());
		URI uri = client.reconstructURI(serviceInstance,
				new URL("http://" + server.getServiceId()).toURI());
		assertEquals(server.getHost(), uri.getHost());
		assertEquals(server.getPort(), uri.getPort());
		assertEquals("https", uri.getScheme());
	}

	@Test
	public void testReconstructSecureUriWithoutScheme() throws Exception {
		testReconstructSchemelessUriWithoutClientConfig(getSecureRibbonServer(), "https");
	}

	@Test
	public void testReconstructUnsecureSchemelessUri() throws Exception {
		testReconstructSchemelessUriWithoutClientConfig(getRibbonServer(), "http");
	}

	public void testReconstructSchemelessUriWithoutClientConfig(RibbonServer server, String expectedScheme)
			throws Exception {
		IClientConfig config = mock(IClientConfig.class);
		when(config.get(CommonClientConfigKey.IsSecure)).thenReturn(null);
		when(clientFactory.getClientConfig(server.getServiceId())).thenReturn(config);

		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
		ServiceInstance serviceInstance = client.choose(server.getServiceId());
		URI uri = client.reconstructURI(serviceInstance,
				new URI("//" + server.getServiceId()));
		assertEquals(server.getHost(), uri.getHost());
		assertEquals(server.getPort(), uri.getPort());
		assertEquals(expectedScheme, uri.getScheme());
	}

	@Test
	public void testChoose() {
		RibbonServer server = getRibbonServer();
		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
		ServiceInstance serviceInstance = client.choose(server.getServiceId());
		assertServiceInstance(server, serviceInstance);
		verify(this.loadBalancer).chooseServer(eq("default"));
	}

	@Test
	public void testChooseWithHint() {
		Object hint = new Object();
		RibbonServer server = getRibbonServer();
		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
		ServiceInstance serviceInstance = client.choose(server.getServiceId(), hint);
		assertServiceInstance(server, serviceInstance);
		verify(this.loadBalancer).chooseServer(same(hint));
	}

	@Test
	public void testChooseMissing() {
		given(this.clientFactory.getLoadBalancer(this.loadBalancer.getName()))
				.willReturn(null);
		given(this.loadBalancer.getName()).willReturn("missingservice");
		RibbonLoadBalancerClient client = new RibbonLoadBalancerClient(this.clientFactory);
		ServiceInstance instance = client.choose("missingservice");
		assertNull("instance wasn't null", instance);
	}

	@Test
	public void testExecute() throws IOException {
		final RibbonServer server = getRibbonServer();
		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
		final String returnVal = "myval";
		Object actualReturn = client.execute(server.getServiceId(),
				(LoadBalancerRequest<Object>) instance -> {
                    assertServiceInstance(server, instance);
                    return returnVal;
                });
		verifyServerStats();
		verify(this.loadBalancer).chooseServer(eq("default"));
		assertEquals("retVal was wrong", returnVal, actualReturn);
	}

	@Test
	public void testExecuteWithHint() throws IOException {
		Object hint = new Object();
		final RibbonServer server = getRibbonServer();
		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
		final String returnVal = "myval";
		Object actualReturn = client.execute(server.getServiceId(),
				(LoadBalancerRequest<Object>) instance -> {
					assertServiceInstance(server, instance);
					return returnVal;
				}, hint);
		verifyServerStats();
		verify(this.loadBalancer).chooseServer(same(hint));
		assertEquals("retVal was wrong", returnVal, actualReturn);
	}

	@Test
	public void testExecuteException() {
		final RibbonServer ribbonServer = getRibbonServer();
		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(ribbonServer);
		try {
			client.execute(ribbonServer.getServiceId(),
					instance -> {
                        assertServiceInstance(ribbonServer, instance);
                        throw new RuntimeException();
                    });
			fail("Should have thrown exception");
		}
		catch (Exception ex) {
			assertNotNull(ex);
		}
		verifyServerStats();
	}

	@Test
	public void testExecuteIOException() {
		final RibbonServer ribbonServer = getRibbonServer();
		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(ribbonServer);
		try {
			client.execute(ribbonServer.getServiceId(),
					instance -> {
                        assertServiceInstance(ribbonServer, instance);
                        throw new IOException();
                    });
			fail("Should have thrown exception");
		}
		catch (Exception ex) {
		    assertThat(ex).isInstanceOf(IOException.class);
		}
		verifyServerStats();
	}

	protected RibbonServer getRibbonServer() {
		return new RibbonServer("testService", new MyServer("myhost", 9080), false,
				Collections.singletonMap("mykey", "myvalue"));
	}

	protected RibbonServer getSecureRibbonServer() {
		return new RibbonServer("testService", new MyServer("myhost", 8443), false,
				Collections.singletonMap("mykey", "myvalue"));
	}

	protected static class MyServer extends Server {

		public MyServer(String host, int port) {
			super(host, port);
		}

	}

	protected void verifyServerStats() {
		verify(this.serverStats).incrementActiveRequestsCount();
		verify(this.serverStats).decrementActiveRequestsCount();
		verify(this.serverStats).incrementNumRequests();
		verify(this.serverStats).noteResponseTime(anyDouble());
	}

	protected void assertServiceInstance(RibbonServer ribbonServer,
			ServiceInstance instance) {
		assertNotNull("instance was null", instance);
		assertEquals("instanceId was wrong", ribbonServer.getInstanceId(),
				instance.getInstanceId());
		assertEquals("serviceId was wrong", ribbonServer.getServiceId(),
				instance.getServiceId());
		assertEquals("host was wrong", ribbonServer.getHost(), instance.getHost());
		assertEquals("port was wrong", ribbonServer.getPort(), instance.getPort());
		assertEquals("missing metadata", ribbonServer.getMetadata().get("mykey"),
				instance.getMetadata().get("mykey"));
	}

	protected RibbonLoadBalancerClient getRibbonLoadBalancerClient(
			RibbonServer ribbonServer) {
		given(this.loadBalancer.getName()).willReturn(ribbonServer.getServiceId());
		given(this.loadBalancer.chooseServer(anyObject())).willReturn(
				ribbonServer.getServer());
		given(this.loadBalancer.getLoadBalancerStats())
				.willReturn(this.loadBalancerStats);
		given(this.loadBalancerStats.getSingleServerStat(ribbonServer.getServer()))
				.willReturn(this.serverStats);
		given(this.clientFactory.getLoadBalancer(this.loadBalancer.getName()))
				.willReturn(this.loadBalancer);
		return new RibbonLoadBalancerClient(this.clientFactory);
	}
}
