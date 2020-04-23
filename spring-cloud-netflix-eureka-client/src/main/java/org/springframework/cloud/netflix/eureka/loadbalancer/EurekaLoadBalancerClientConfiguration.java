/*
 * Copyright 2013-2020 the original author or authors.
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

import javax.annotation.PostConstruct;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.EurekaClientConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig;
import org.springframework.cloud.netflix.eureka.support.ZoneUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.netflix.eureka.loadbalancer.LoadBalancerEurekaAutoConfiguration.LOADBALANCER_ZONE;

/**
 * A configuration for Spring Cloud LoadBalancer that retrieves client instance zone from
 * Eureka and sets it as a property. Based on
 * {@link EurekaLoadBalancerClientConfiguration}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.1
 * @see EurekaLoadBalancerClientConfiguration
 */
@Configuration
@ConditionalOnBean({ LoadBalancerZoneConfig.class, EurekaLoadBalancerProperties.class })
public class EurekaLoadBalancerClientConfiguration {

	private static final Log LOG = LogFactory
			.getLog(EurekaLoadBalancerClientConfiguration.class);

	private final EurekaClientConfig clientConfig;

	private final EurekaInstanceConfig eurekaConfig;

	private final LoadBalancerZoneConfig zoneConfig;

	private final EurekaLoadBalancerProperties eurekaLoadBalancerProperties;

	public EurekaLoadBalancerClientConfiguration(
			@Autowired(required = false) EurekaClientConfig clientConfig,
			@Autowired(required = false) EurekaInstanceConfig eurekaInstanceConfig,
			LoadBalancerZoneConfig zoneConfig,
			EurekaLoadBalancerProperties eurekaLoadBalancerProperties) {
		this.clientConfig = clientConfig;
		this.eurekaConfig = eurekaInstanceConfig;
		this.zoneConfig = zoneConfig;
		this.eurekaLoadBalancerProperties = eurekaLoadBalancerProperties;
	}

	@PostConstruct
	public void postprocess() {
		if (!StringUtils.isEmpty(zoneConfig.getZone())) {
			return;
		}
		String zone = getZoneFromEureka();
		if (!StringUtils.isEmpty(zone)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Setting the value of '" + LOADBALANCER_ZONE + "' to " + zone);
			}
			zoneConfig.setZone(zone);
		}
	}

	private String getZoneFromEureka() {
		String zone;
		boolean approximateZoneFromHostname = eurekaLoadBalancerProperties
				.isApproximateZoneFromHostname();
		if (approximateZoneFromHostname && eurekaConfig != null) {
			return ZoneUtils.extractApproximateZone(this.eurekaConfig.getHostName(false));
		}
		else {
			zone = eurekaConfig == null ? null
					: eurekaConfig.getMetadataMap().get("zone");
			if (StringUtils.isEmpty(zone) && clientConfig != null) {
				String[] zones = clientConfig
						.getAvailabilityZones(clientConfig.getRegion());
				// Pick the first one from the regions we want to connect to
				zone = zones != null && zones.length > 0 ? zones[0] : null;
			}
			return zone;
		}
	}

}
