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

package org.springframework.cloud.netflix.ribbon;

import java.util.ArrayList;
import java.util.List;

import com.netflix.client.config.IClientConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext.ContextKey;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAffinityServerListFilter;

/**
 * A filter that actively prefers the local zone (as defined by the deployment context, or
 * the Eureka instance metadata).
 *
 * @author Dave Syer
 */
public class ZonePreferenceServerListFilter extends ZoneAffinityServerListFilter<Server> {

	private String zone;

	public ZonePreferenceServerListFilter() {
	}

	@Override
	public void initWithNiwsConfig(IClientConfig niwsClientConfig) {
		super.initWithNiwsConfig(niwsClientConfig);
		if (ConfigurationManager.getDeploymentContext() != null) {
			this.zone = ConfigurationManager.getDeploymentContext().getValue(
					ContextKey.zone);
		}
	}

	@Override
	public List<Server> getFilteredListOfServers(List<Server> servers) {
		List<Server> output = super.getFilteredListOfServers(servers);
		if (this.zone != null && output.size() == servers.size()) {
			List<Server> local = new ArrayList<Server>();
			for (Server server : output) {
				if (this.zone.equalsIgnoreCase(server.getZone())) {
					local.add(server);
				}
			}
			if (!local.isEmpty()) {
				return local;
			}
		}
		return output;
	}

	public String getZone() {
		return this.zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.ribbon.ZonePreferenceServerListFilter(zone="
				+ this.zone + ")";
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof ZonePreferenceServerListFilter))
			return false;
		final ZonePreferenceServerListFilter other = (ZonePreferenceServerListFilter) o;
		if (!other.canEqual((Object) this))
			return false;
		final Object this$zone = this.getZone();
		final Object other$zone = other.getZone();
		if (this$zone == null ? other$zone != null : !this$zone.equals(other$zone))
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $zone = this.getZone();
		result = result * PRIME + ($zone == null ? 0 : $zone.hashCode());
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof ZonePreferenceServerListFilter;
	}
}
