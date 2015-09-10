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

package org.springframework.cloud.netflix.ribbon.eureka;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Spencer Gibb
 */
public class DomainExtractingServerListTests {

	static final String IP_ADDR = "10.0.0.2";

	static final int PORT = 8080;

	static final String ZONE = "myzone.mydomain.com";

	static final String HOST_NAME = "myHostName." + ZONE;

	static final String INSTANCE_ID = "myInstanceId";

	private Map<String, String> metadata = Collections.<String, String> singletonMap(
			"instanceId", INSTANCE_ID);

	@Test
	public void testDomainExtractingServer() {
		DomainExtractingServerList serverList = getDomainExtractingServerList(
				new DefaultClientConfigImpl(), true);
		List<DiscoveryEnabledServer> servers = serverList.getInitialListOfServers();
		assertNotNull("servers was null", servers);
		assertEquals("servers was not size 1", 1, servers.size());
		DomainExtractingServer des = assertDomainExtractingServer(servers, ZONE);
		assertEquals("hostPort was wrong", HOST_NAME + ":" + PORT, des.getHostPort());
	}

	@Test
	public void testZoneInMetaData() {
		this.metadata = new HashMap<String, String>();
		this.metadata.put("zone", "us-west-1");
		this.metadata.put("instanceId", INSTANCE_ID);
		DomainExtractingServerList serverList = getDomainExtractingServerList(
				new DefaultClientConfigImpl(), false);
		List<DiscoveryEnabledServer> servers = serverList.getInitialListOfServers();
		assertNotNull("servers was null", servers);
		assertEquals("servers was not size 1", 1, servers.size());
		DomainExtractingServer des = assertDomainExtractingServer(servers, "us-west-1");
		assertEquals("Zone was wrong", "us-west-1", des.getZone());
	}

	@Test
	public void testDomainExtractingServerDontApproximateZone() {
		DomainExtractingServerList serverList = getDomainExtractingServerList(
				new DefaultClientConfigImpl(), false);
		List<DiscoveryEnabledServer> servers = serverList.getInitialListOfServers();
		assertNotNull("servers was null", servers);
		assertEquals("servers was not size 1", 1, servers.size());
		DomainExtractingServer des = assertDomainExtractingServer(servers, null);
		assertEquals("hostPort was wrong", HOST_NAME + ":" + PORT, des.getHostPort());
	}

	protected DomainExtractingServer assertDomainExtractingServer(
			List<DiscoveryEnabledServer> servers, String zone) {
		Server actualServer = servers.get(0);
		assertTrue("server was not a DomainExtractingServer",
				actualServer instanceof DomainExtractingServer);
		DomainExtractingServer des = DomainExtractingServer.class.cast(actualServer);
		assertEquals("zone was wrong", zone, des.getZone());
		assertEquals("instanceId was wrong", HOST_NAME + ":" + INSTANCE_ID, des.getId());
		return des;
	}

	@Test
	public void testDomainExtractingServerUseIpAddress() {
		DefaultClientConfigImpl config = new DefaultClientConfigImpl();
		config.setProperty(CommonClientConfigKey.UseIPAddrForServer, true);
		DomainExtractingServerList serverList = getDomainExtractingServerList(config,
				true);
		List<DiscoveryEnabledServer> servers = serverList.getInitialListOfServers();
		assertNotNull("servers was null", servers);
		assertEquals("servers was not size 1", 1, servers.size());
		DomainExtractingServer des = assertDomainExtractingServer(servers, ZONE);
		assertEquals("hostPort was wrong", IP_ADDR + ":" + PORT, des.getHostPort());
	}

	protected DomainExtractingServerList getDomainExtractingServerList(
			DefaultClientConfigImpl config, boolean approximateZoneFromHostname) {
		DiscoveryEnabledServer server = mock(DiscoveryEnabledServer.class);
		@SuppressWarnings("unchecked")
		ServerList<DiscoveryEnabledServer> originalServerList = mock(ServerList.class);
		InstanceInfo instanceInfo = mock(InstanceInfo.class);
		given(server.getInstanceInfo()).willReturn(instanceInfo);
		given(server.getHost()).willReturn(HOST_NAME);
		given(instanceInfo.getMetadata()).willReturn(this.metadata);
		given(instanceInfo.getHostName()).willReturn(HOST_NAME);
		given(instanceInfo.getIPAddr()).willReturn(IP_ADDR);
		given(instanceInfo.getPort()).willReturn(PORT);
		given(originalServerList.getInitialListOfServers()).willReturn(
				Arrays.asList(server));
		return new DomainExtractingServerList(originalServerList, config,
				approximateZoneFromHostname);
	}

}
