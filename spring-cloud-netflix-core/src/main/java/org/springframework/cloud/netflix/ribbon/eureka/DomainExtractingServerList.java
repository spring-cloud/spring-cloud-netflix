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

import lombok.Getter;
import lombok.Setter;

import org.springframework.util.StringUtils;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

/**
 * @author Dave Syer
 *
 */
public class DomainExtractingServerList implements ServerList<Server> {

	private ServerList<Server> list;
	private IClientConfig clientConfig;
	private boolean approximateZoneFromHostname;

	public DomainExtractingServerList(ServerList<Server> list,
			IClientConfig clientConfig, boolean approximateZoneFromHostname) {
		this.list = list;
		this.clientConfig = clientConfig;
		this.approximateZoneFromHostname = approximateZoneFromHostname;
	}

	@Override
	public List<Server> getInitialListOfServers() {
		List<Server> servers = setZones(this.list.getInitialListOfServers());
		return servers;
	}

	@Override
	public List<Server> getUpdatedListOfServers() {
		List<Server> servers = setZones(this.list.getUpdatedListOfServers());
		return servers;
	}

	private List<Server> setZones(List<Server> servers) {
		List<Server> result = new ArrayList<>();
		boolean isSecure = this.clientConfig.getPropertyAsBoolean(
				CommonClientConfigKey.IsSecure, Boolean.TRUE);
		boolean shouldUseIpAddr = this.clientConfig.getPropertyAsBoolean(
				CommonClientConfigKey.UseIPAddrForServer, Boolean.FALSE);
		for (Server server : servers) {
			if (server instanceof DiscoveryEnabledServer) {
				result.add(new DomainExtractingServer((DiscoveryEnabledServer) server,
						isSecure, shouldUseIpAddr, this.approximateZoneFromHostname));
			}
			else {
				result.add(server);
			}
		}
		return result;
	}

}

class DomainExtractingServer extends DiscoveryEnabledServer {

	@Getter
	@Setter
	private String id;

	public DomainExtractingServer(DiscoveryEnabledServer server, boolean useSecurePort,
			boolean useIpAddr, boolean approximateZoneFromHostname) {
		// host and port are set in super()
		super(server.getInstanceInfo(), useSecurePort, useIpAddr);
		if (approximateZoneFromHostname) {
			setZone(extractApproximateZone(server));
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
				return instance.getMetadata().get("instanceId");
			}
		}
		return super.getId();
	}

	private String extractApproximateZone(Server server) {
		String host = server.getHost();
		if (!host.contains(".")) {
			return host;
		}
		String[] split = StringUtils.split(host, ".");
		StringBuilder builder = new StringBuilder(split[1]);
		for (int i = 2; i < split.length; i++) {
			builder.append(".").append(split[i]);
		}
		return builder.toString();
	}

}
