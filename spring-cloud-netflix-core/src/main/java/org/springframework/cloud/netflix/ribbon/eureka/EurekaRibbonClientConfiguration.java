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

import static com.netflix.client.config.CommonClientConfigKey.DeploymentContextBasedVipAddresses;
import static com.netflix.client.config.CommonClientConfigKey.EnableZoneAffinity;

import javax.annotation.PostConstruct;

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.netflix.client.config.IClientConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext.ContextKey;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.ServerList;
import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList;
import com.netflix.niws.loadbalancer.NIWSDiscoveryPing;

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
@CommonsLog
public class EurekaRibbonClientConfiguration {

	@Value("${ribbon.eureka.approximateZoneFromHostname:false}")
	private boolean approximateZoneFromHostname = false;

	@Value("${ribbon.client.name}")
	private String serviceId = "client";

	protected static final String VALUE_NOT_SET = "__not__set__";

	protected static final String DEFAULT_NAMESPACE = "ribbon";

	@Autowired(required = false)
	private EurekaClientConfig clientConfig;

	@Autowired
	private EurekaInstanceConfigBean eurekaConfig;

	public EurekaRibbonClientConfiguration() {
	}

	public EurekaRibbonClientConfiguration(EurekaClientConfig clientConfig,
			String serviceId, EurekaInstanceConfigBean eurekaConfig,
			boolean approximateZoneFromHostname) {
		this.clientConfig = clientConfig;
		this.serviceId = serviceId;
		this.eurekaConfig = eurekaConfig;
		this.approximateZoneFromHostname = approximateZoneFromHostname;
	}

	@Bean
	@ConditionalOnMissingBean
	public IPing ribbonPing(IClientConfig config) {
		NIWSDiscoveryPing ping = new NIWSDiscoveryPing();
		ping.initWithNiwsConfig(config);
		return ping;
	}

	@Bean
	@ConditionalOnMissingBean
	public ServerList<?> ribbonServerList(IClientConfig config) {
		DiscoveryEnabledNIWSServerList discoveryServerList = new DiscoveryEnabledNIWSServerList(
				config);
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
		String zone = ConfigurationManager.getDeploymentContext().getValue(
				ContextKey.zone);
		if (this.clientConfig != null && StringUtils.isEmpty(zone)) {
			if (approximateZoneFromHostname) {
				String approxZone = ZoneUtils.extractApproximateZone(eurekaConfig
						.getHostname());
				log.debug("Setting Zone To " + approxZone);
				ConfigurationManager.getDeploymentContext().setValue(ContextKey.zone,
						approxZone);
			}
			else {
				String[] zones = this.clientConfig.getAvailabilityZones(this.clientConfig
						.getRegion());
				String availabilityZone = zones != null && zones.length > 0 ? zones[0]
						: null;
				if (availabilityZone != null) {
					// You can set this with archaius.deployment.* (maybe requires
					// custom deployment context)?
					ConfigurationManager.getDeploymentContext().setValue(ContextKey.zone,
							availabilityZone);
				}
			}
		}
		setProp(this.serviceId, DeploymentContextBasedVipAddresses.key(), this.serviceId);
		setProp(this.serviceId, EnableZoneAffinity.key(), "true");
	}

	protected void setProp(String serviceId, String suffix, String value) {
		// how to set the namespace properly?
		String key = getKey(serviceId, suffix);
		DynamicStringProperty property = getProperty(key);
		if (property.get().equals(VALUE_NOT_SET)) {
			ConfigurationManager.getConfigInstance().setProperty(key, value);
		}
	}

	protected DynamicStringProperty getProperty(String key) {
		return DynamicPropertyFactory.getInstance().getStringProperty(key, VALUE_NOT_SET);
	}

	protected String getKey(String serviceId, String suffix) {
		return serviceId + "." + DEFAULT_NAMESPACE + "." + suffix;
	}

}
