/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

	private Map<String, String> metadata = Collections
			.<String, String>singletonMap("instanceId", INSTANCE_ID);

	@Test
	public void testDomainExtractingServer() {
		DomainExtractingServerList serverList = getDomainExtractingServerList(
				new DefaultClientConfigImpl(), true);
		List<DiscoveryEnabledServer> servers = serverList.getInitialListOfServers();
		assertThat(servers).as("servers was null").isNotNull();
		assertThat(servers.size()).as("servers was not size 1").isEqualTo(1);
		DomainExtractingServer des = assertDomainExtractingServer(servers, ZONE);
		assertThat(des.getHostPort()).as("hostPort was wrong")
				.isEqualTo(HOST_NAME + ":" + PORT);
	}

	@Test
	public void testZoneInMetaData() {
		this.metadata = new HashMap<>();
		this.metadata.put("zone", "us-west-1");
		this.metadata.put("instanceId", INSTANCE_ID);
		DomainExtractingServerList serverList = getDomainExtractingServerList(
				new DefaultClientConfigImpl(), false);
		List<DiscoveryEnabledServer> servers = serverList.getInitialListOfServers();
		assertThat(servers).as("servers was null").isNotNull();
		assertThat(servers.size()).as("servers was not size 1").isEqualTo(1);
		DomainExtractingServer des = assertDomainExtractingServer(servers, "us-west-1");
		assertThat(des.getZone()).as("Zone was wrong").isEqualTo("us-west-1");
	}

	@Test
	public void testDomainExtractingServerDontApproximateZone() {
		DomainExtractingServerList serverList = getDomainExtractingServerList(
				new DefaultClientConfigImpl(), false);
		List<DiscoveryEnabledServer> servers = serverList.getInitialListOfServers();
		assertThat(servers).as("servers was null").isNotNull();
		assertThat(servers.size()).as("servers was not size 1").isEqualTo(1);
		DomainExtractingServer des = assertDomainExtractingServer(servers, null);
		assertThat(des.getHostPort()).as("hostPort was wrong")
				.isEqualTo(HOST_NAME + ":" + PORT);
	}

	protected DomainExtractingServer assertDomainExtractingServer(
			List<DiscoveryEnabledServer> servers, String zone) {
		Server actualServer = servers.get(0);
		assertThat(actualServer instanceof DomainExtractingServer)
				.as("server was not a DomainExtractingServer").isTrue();
		DomainExtractingServer des = DomainExtractingServer.class.cast(actualServer);
		assertThat(des.getZone()).as("zone was wrong").isEqualTo(zone);
		assertThat(des.getId()).as("instanceId was wrong")
				.isEqualTo(HOST_NAME + ":" + INSTANCE_ID);
		return des;
	}

	@Test
	public void testDomainExtractingServerUseIpAddress() {
		DefaultClientConfigImpl config = new DefaultClientConfigImpl();
		config.setProperty(CommonClientConfigKey.UseIPAddrForServer, true);
		DomainExtractingServerList serverList = getDomainExtractingServerList(config,
				true);
		List<DiscoveryEnabledServer> servers = serverList.getInitialListOfServers();
		assertThat(servers).as("servers was null").isNotNull();
		assertThat(servers.size()).as("servers was not size 1").isEqualTo(1);
		DomainExtractingServer des = assertDomainExtractingServer(servers, ZONE);
		assertThat(des.getHostPort()).as("hostPort was wrong")
				.isEqualTo(IP_ADDR + ":" + PORT);
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
		given(originalServerList.getInitialListOfServers())
				.willReturn(Arrays.asList(server));
		return new DomainExtractingServerList(originalServerList, config,
				approximateZoneFromHostname);
	}

}
