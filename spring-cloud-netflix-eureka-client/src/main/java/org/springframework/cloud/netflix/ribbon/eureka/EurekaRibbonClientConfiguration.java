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

import javax.annotation.PostConstruct;
import javax.inject.Provider;

import com.netflix.discovery.EurekaClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.ribbon.PropertiesFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.client.config.IClientConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext.ContextKey;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.ServerList;
import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList;
import com.netflix.niws.loadbalancer.NIWSDiscoveryPing;

import static com.netflix.client.config.CommonClientConfigKey.DeploymentContextBasedVipAddresses;
import static com.netflix.client.config.CommonClientConfigKey.EnableZoneAffinity;
import static org.springframework.cloud.netflix.ribbon.RibbonUtils.setRibbonProperty;

/**
 * Preprocessor that configures defaults for eureka-discovered ribbon clients. Such as:
 * <code>@zone</code>, NIWSServerListClassName, DeploymentContextBasedVipAddresses,
 * NFLoadBalancerRuleClassName, NIWSServerListFilterClassName and more
 *
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Ryan Baxter
 */
@Configuration
public class EurekaRibbonClientConfiguration {

	private static final Log log = LogFactory.getLog(EurekaRibbonClientConfiguration.class);

	@Value("${ribbon.eureka.approximateZoneFromHostname:false}")
	private boolean approximateZoneFromHostname = false;

	@Value("${ribbon.client.name}")
	private String serviceId = "client";

	@Autowired(required = false)
	private EurekaClientConfig clientConfig;

	@Autowired(required = false)
	private EurekaInstanceConfig eurekaConfig;

	@Autowired
	private PropertiesFactory propertiesFactory;

	public EurekaRibbonClientConfiguration() {
	}

	public EurekaRibbonClientConfiguration(EurekaClientConfig clientConfig,
			String serviceId, EurekaInstanceConfig eurekaConfig,
			boolean approximateZoneFromHostname) {
		this.clientConfig = clientConfig;
		this.serviceId = serviceId;
		this.eurekaConfig = eurekaConfig;
		this.approximateZoneFromHostname = approximateZoneFromHostname;
	}

	@Bean
	@ConditionalOnMissingBean
	public IPing ribbonPing(IClientConfig config) {
		if (this.propertiesFactory.isSet(IPing.class, serviceId)) {
			return this.propertiesFactory.get(IPing.class, config, serviceId);
		}
		NIWSDiscoveryPing ping = new NIWSDiscoveryPing();
		ping.initWithNiwsConfig(config);
		return ping;
	}

	@Bean
	@ConditionalOnMissingBean
	public ServerList<?> ribbonServerList(IClientConfig config, Provider<EurekaClient> eurekaClientProvider) {
		if (this.propertiesFactory.isSet(ServerList.class, serviceId)) {
			return this.propertiesFactory.get(ServerList.class, config, serviceId);
		}
		DiscoveryEnabledNIWSServerList discoveryServerList = new DiscoveryEnabledNIWSServerList(
				config, eurekaClientProvider);
		DomainExtractingServerList serverList = new DomainExtractingServerList(
				discoveryServerList, config, this.approximateZoneFromHostname);
		return serverList;
	}

	@Bean
	public ServerIntrospector serverIntrospector() {
		return new EurekaServerIntrospector();
	}

	@PostConstruct
	public void preprocess() {
		String zone = ConfigurationManager.getDeploymentContext()
				.getValue(ContextKey.zone);
		if (this.clientConfig != null && StringUtils.isEmpty(zone)) {
			if (this.approximateZoneFromHostname && this.eurekaConfig != null) {
				String approxZone = ZoneUtils
						.extractApproximateZone(this.eurekaConfig.getHostName(false));
				log.debug("Setting Zone To " + approxZone);
				ConfigurationManager.getDeploymentContext().setValue(ContextKey.zone,
						approxZone);
			}
			else {
				String availabilityZone = this.eurekaConfig == null ? null
						: this.eurekaConfig.getMetadataMap().get("zone");
				if (availabilityZone == null) {
					String[] zones = this.clientConfig
							.getAvailabilityZones(this.clientConfig.getRegion());
					// Pick the first one from the regions we want to connect to
					availabilityZone = zones != null && zones.length > 0 ? zones[0]
							: null;
				}
				if (availabilityZone != null) {
					// You can set this with archaius.deployment.* (maybe requires
					// custom deployment context)?
					ConfigurationManager.getDeploymentContext().setValue(ContextKey.zone,
							availabilityZone);
				}
			}
		}
		setRibbonProperty(this.serviceId, DeploymentContextBasedVipAddresses.key(),
				this.serviceId);
		setRibbonProperty(this.serviceId, EnableZoneAffinity.key(), "true");
	}

}
