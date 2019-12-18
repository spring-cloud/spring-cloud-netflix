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

package org.springframework.cloud.netflix.eureka.loadbalancer;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.EurekaClientConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.ribbon.eureka.ZoneUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

/**
 * A configuration for Spring Cloud LoadBalancer that retrieves client instance zone from
 * Eureka and sets it as a property. Based on
 * {@link EurekaLoadBalancerClientConfiguration}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.1
 * @see EurekaLoadBalancerClientConfiguration
 */
public class EurekaLoadBalancerClientConfiguration {

	// Visible for tests
	static final String LOADBALANCER_ZONE = "spring.cloud.loadbalancer.zone";

	// Visible for tests
	static final String APPROXIMATE_ZONE_FROM_HOSTNAME = "spring.cloud.loadbalancer.eureka.approximateZoneFromHostname";

	private static final Log LOG = LogFactory
			.getLog(EurekaLoadBalancerClientConfiguration.class);

	private final EurekaClientConfig clientConfig;

	private final EurekaInstanceConfig eurekaConfig;

	private final ConfigurableEnvironment environment;

	public EurekaLoadBalancerClientConfiguration(
			@Autowired(required = false) EurekaClientConfig clientConfig,
			@Autowired(required = false) EurekaInstanceConfig eurekaInstanceConfig,
			ConfigurableEnvironment environment) {
		this.clientConfig = clientConfig;
		this.eurekaConfig = eurekaInstanceConfig;
		this.environment = environment;
	}

	@PostConstruct
	public void preprocess() {
		if (!StringUtils.isEmpty(environment.getProperty(LOADBALANCER_ZONE))) {
			return;
		}
		String zone = getZoneFromEureka();
		if (zone != null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Setting the value of '" + LOADBALANCER_ZONE + "' to " + zone);
			}
			addZoneProperty(zone);
		}
	}

	private void addZoneProperty(String zone) {
		MutablePropertySources propertySources = environment.getPropertySources();
		Map<String, Object> myMap = new HashMap<>();
		myMap.put(LOADBALANCER_ZONE, zone);
		propertySources.addLast(new MapPropertySource("LOADBALANCER_MAP", myMap));
	}

	private String getZoneFromEureka() {
		String zone;
		boolean approximateZoneFromHostname = Boolean.parseBoolean(
				environment.getProperty(APPROXIMATE_ZONE_FROM_HOSTNAME, "false"));
		if (approximateZoneFromHostname && eurekaConfig != null) {
			return ZoneUtils.extractApproximateZone(this.eurekaConfig.getHostName(false));
		}
		else {
			zone = eurekaConfig == null ? null
					: eurekaConfig.getMetadataMap().get("zone");
			if (zone == null) {
				String[] zones = clientConfig
						.getAvailabilityZones(clientConfig.getRegion());
				// Pick the first one from the regions we want to connect to
				zone = zones != null && zones.length > 0 ? zones[0] : null;
			}
			return zone;
		}
	}

}
