/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

/**
 * @author Dave Syer
 */
public class DomainExtractingServerList implements ServerList<DiscoveryEnabledServer> {

	private ServerList<DiscoveryEnabledServer> list;

	private IClientConfig clientConfig;

	private boolean approximateZoneFromHostname;

	public DomainExtractingServerList(ServerList<DiscoveryEnabledServer> list,
			IClientConfig clientConfig, boolean approximateZoneFromHostname) {
		this.list = list;
		this.clientConfig = clientConfig;
		this.approximateZoneFromHostname = approximateZoneFromHostname;
	}

	@Override
	public List<DiscoveryEnabledServer> getInitialListOfServers() {
		List<DiscoveryEnabledServer> servers = setZones(this.list
				.getInitialListOfServers());
		return servers;
	}

	@Override
	public List<DiscoveryEnabledServer> getUpdatedListOfServers() {
		List<DiscoveryEnabledServer> servers = setZones(this.list
				.getUpdatedListOfServers());
		return servers;
	}

	private List<DiscoveryEnabledServer> setZones(List<DiscoveryEnabledServer> servers) {
		List<DiscoveryEnabledServer> result = new ArrayList<>();
		boolean isSecure = this.clientConfig.getPropertyAsBoolean(
				CommonClientConfigKey.IsSecure, Boolean.TRUE);
		boolean shouldUseIpAddr = this.clientConfig.getPropertyAsBoolean(
				CommonClientConfigKey.UseIPAddrForServer, Boolean.FALSE);
		for (DiscoveryEnabledServer server : servers) {
			result.add(new DomainExtractingServer(server, isSecure, shouldUseIpAddr,
					this.approximateZoneFromHostname));
		}
		return result;
	}

}

class DomainExtractingServer extends DiscoveryEnabledServer {

	private String id;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public DomainExtractingServer(DiscoveryEnabledServer server, boolean useSecurePort,
			boolean useIpAddr, boolean approximateZoneFromHostname) {
		// host and port are set in super()
		super(server.getInstanceInfo(), useSecurePort, useIpAddr);
		if (server.getInstanceInfo().getMetadata().containsKey("zone")) {
			setZone(server.getInstanceInfo().getMetadata().get("zone"));
		}
		else if (approximateZoneFromHostname) {
			setZone(ZoneUtils.extractApproximateZone(server.getHost()));
		}
		else {
			setZone(server.getZone());
		}
		setId(extractId(server));
		setAlive(server.isAlive());
		setReadyToServe(server.isReadyToServe());
	}

	private String extractId(Server server) {
		if (server instanceof DiscoveryEnabledServer) {
			DiscoveryEnabledServer enabled = (DiscoveryEnabledServer) server;
			InstanceInfo instance = enabled.getInstanceInfo();
			if (instance.getMetadata().containsKey("instanceId")) {
				return instance.getHostName()+":"+instance.getMetadata().get("instanceId");
			}
		}
		return super.getId();
	}
}
