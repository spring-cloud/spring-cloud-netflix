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
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

/**
 * @author Dave Syer
 *
 */
public class DomainExtractingServerList implements ServerList<Server> {

	private ServerList<Server> list;

	public DomainExtractingServerList(ServerList<Server> list) {
		this.list = list;
	}

	@Override
	public List<Server> getInitialListOfServers() {
		List<Server> servers = setZones(list.getInitialListOfServers());
		return servers;
	}

	@Override
	public List<Server> getUpdatedListOfServers() {
		List<Server> servers = setZones(list.getUpdatedListOfServers());
		return servers;
	}

	private List<Server> setZones(List<Server> servers) {
		List<Server> result = new ArrayList<Server>();
		for (Server server : servers) {
			if (server instanceof DiscoveryEnabledServer) {
				result.add(new DomainExtractingServer((DiscoveryEnabledServer) server));
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

	public DomainExtractingServer(DiscoveryEnabledServer server) {
		super(server.getInstanceInfo(), true, false);
		String zone = extractApproximateZone(server);
		setZone(zone);
		String id = extractId(server);
		setId(id);
	}

	private String extractId(Server server) {
		if (server instanceof DiscoveryEnabledServer) {
			DiscoveryEnabledServer enabled = (DiscoveryEnabledServer) server;
			InstanceInfo instance = enabled.getInstanceInfo();
			if (instance.getMetadata().containsKey("instanceId")) {
				return instance.getMetadata().get("instanceId");
			}
		}
		return server.getId();
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
