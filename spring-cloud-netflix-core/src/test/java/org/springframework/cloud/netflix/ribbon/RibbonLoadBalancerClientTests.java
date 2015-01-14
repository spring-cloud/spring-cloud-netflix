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

import java.net.URI;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient.RibbonServer;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.LoadBalancerStats;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerStats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Spencer Gibb
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

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(this.clientFactory.getLoadBalancerContext(anyString())).thenReturn(
				new RibbonLoadBalancerContext(this.loadBalancer));
	}

	@Test
	public void reconstructURI() throws Exception {
		RibbonServer server = getRibbonServer();
		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
		ServiceInstance serviceInstance = client.choose(server.getServiceId());
		URI uri = client.reconstructURI(serviceInstance, new URL("http://"
				+ server.serviceId).toURI());
		assertEquals(server.getHost(), uri.getHost());
		assertEquals(server.getPort(), uri.getPort());
	}

	@Test
	public void testChoose() {
		RibbonServer server = getRibbonServer();
		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
		ServiceInstance serviceInstance = client.choose(server.getServiceId());
		assertServiceInstance(server, serviceInstance);
	}

	@Test
	public void testExecute() {
		final RibbonServer server = getRibbonServer();
		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
		final String returnVal = "myval";
		Object actualReturn = client.execute(server.getServiceId(),
				new LoadBalancerRequest<Object>() {
					@Override
					public Object apply(ServiceInstance instance) throws Exception {
						assertServiceInstance(server, instance);
						return returnVal;
					}
				});
		verifyServerStats();
		assertEquals("retVal was wrong", returnVal, actualReturn);
	}

	@Test
	public void testExecuteException() {
		final RibbonServer ribbonServer = getRibbonServer();
		RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(ribbonServer);
		try {
			client.execute(ribbonServer.getServiceId(),
					new LoadBalancerRequest<Object>() {
						@Override
						public Object apply(ServiceInstance instance) throws Exception {
							assertServiceInstance(ribbonServer, instance);
							throw new RuntimeException();
						}
					});
			fail("Should have thrown exception");
		}
		catch (Exception ex) {
			assertNotNull(ex);
		}
		verifyServerStats();
	}

	protected RibbonServer getRibbonServer() {
		return new RibbonServer("testService", new Server("myhost", 9080));
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
		assertEquals("serviceId was wrong", ribbonServer.getServiceId(),
				instance.getServiceId());
		assertEquals("host was wrong", ribbonServer.getHost(), instance.getHost());
		assertEquals("port was wrong", ribbonServer.getPort(), instance.getPort());
	}

	protected RibbonLoadBalancerClient getRibbonLoadBalancerClient(
			RibbonServer ribbonServer) {
		when(this.loadBalancer.getName()).thenReturn(ribbonServer.getServiceId());
		when(this.loadBalancer.chooseServer(anyString())).thenReturn(ribbonServer.server);
		when(this.loadBalancer.getLoadBalancerStats()).thenReturn(this.loadBalancerStats);
		when(this.loadBalancerStats.getSingleServerStat(ribbonServer.server)).thenReturn(
				this.serverStats);
		when(this.clientFactory.getLoadBalancer(this.loadBalancer.getName())).thenReturn(
				this.loadBalancer);
		return new RibbonLoadBalancerClient(this.clientFactory);
	}
}
