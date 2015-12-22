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

import java.util.Map;

import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

/**
 * @author Spencer Gibb
 */
public class EurekaServerIntrospector extends DefaultServerIntrospector {

	@Override
	public boolean isSecure(Server server) {
		if (server instanceof DiscoveryEnabledServer) {
			DiscoveryEnabledServer discoveryServer = (DiscoveryEnabledServer) server;
			return discoveryServer.getInstanceInfo().isPortEnabled(InstanceInfo.PortType.SECURE);
		}
		return super.isSecure(server);
	}

	@Override
	public Map<String, String> getMetadata(Server server) {
		if (server instanceof DiscoveryEnabledServer) {
			DiscoveryEnabledServer discoveryServer = (DiscoveryEnabledServer) server;
			return discoveryServer.getInstanceInfo().getMetadata();
		}
		return super.getMetadata(server);
	}
}
