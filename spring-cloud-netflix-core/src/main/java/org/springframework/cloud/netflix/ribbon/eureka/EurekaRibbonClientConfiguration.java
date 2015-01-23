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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.netflix.ribbon.ZonePreferenceServerListFilter;
import org.springframework.context.annotation.Configuration;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext.ContextKey;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList;

import static com.netflix.client.config.CommonClientConfigKey.DeploymentContextBasedVipAddresses;
import static com.netflix.client.config.CommonClientConfigKey.EnableZoneAffinity;
import static com.netflix.client.config.CommonClientConfigKey.NFLoadBalancerRuleClassName;
import static com.netflix.client.config.CommonClientConfigKey.NIWSServerListClassName;
import static com.netflix.client.config.CommonClientConfigKey.NIWSServerListFilterClassName;

/**
 * Preprocessor that configures defaults for eureka-discovered ribbon clients. Such as:
 * <code>@zone</code>, NIWSServerListClassName, DeploymentContextBasedVipAddresses,
 * NFLoadBalancerRuleClassName, NIWSServerListFilterClassName and more
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Configuration
public class EurekaRibbonClientConfiguration implements BeanPostProcessor {

	@Value("${ribbon.eureka.approximateZoneFromHostname:false}")
	private boolean approximateZoneFromHostname = false;

	@Value("${ribbon.client.name}")
	private String serviceId = "client";

	protected static final String VALUE_NOT_SET = "__not__set__";

	protected static final String DEFAULT_NAMESPACE = "ribbon";

	@Autowired(required = false)
	private EurekaClientConfig clientConfig;

	public EurekaRibbonClientConfiguration() {
	}

	public EurekaRibbonClientConfiguration(EurekaClientConfig clientConfig,
			String serviceId) {
		this.clientConfig = clientConfig;
		this.serviceId = serviceId;
	}

	@PostConstruct
	public void preprocess() {
		if (this.clientConfig != null
				&& ConfigurationManager.getDeploymentContext().getValue(ContextKey.zone) == null) {
			String[] zones = this.clientConfig.getAvailabilityZones(this.clientConfig
					.getRegion());
			String zone = zones != null && zones.length > 0 ? zones[0] : null;
			if (zone != null) {
				// You can set this with archaius.deployment.* (maybe requires
				// custom deployment context)?
				ConfigurationManager.getDeploymentContext().setValue(ContextKey.zone,
						zone);
			}
		}
		// TODO: should this look more like hibernate spring boot props?
		setProp(this.serviceId, NIWSServerListClassName.key(),
				DiscoveryEnabledNIWSServerList.class.getName());
		// FIXME: what should this be?
		setProp(this.serviceId, DeploymentContextBasedVipAddresses.key(), this.serviceId);
		setProp(this.serviceId, NFLoadBalancerRuleClassName.key(),
				ZoneAvoidanceRule.class.getName());
		setProp(this.serviceId, NIWSServerListFilterClassName.key(),
				ZonePreferenceServerListFilter.class.getName());
		setProp(this.serviceId, EnableZoneAffinity.key(), "true");
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof DynamicServerListLoadBalancer) {
			wrapServerList((DynamicServerListLoadBalancer<?>) bean);
		}
		return bean;
	}

	private void wrapServerList(DynamicServerListLoadBalancer<?> balancer) {
			@SuppressWarnings("unchecked")
			DynamicServerListLoadBalancer<Server> dynamic = (DynamicServerListLoadBalancer<Server>) balancer;
			ServerList<Server> list = dynamic.getServerListImpl();
			if (!(list instanceof DomainExtractingServerList)) {
				// This is optional: you can use the native Eureka AWS features as long as
				// the server zone is populated. TODO: verify that we back off if AWS
				// metadata *is* available.
				// @see com.netflix.appinfo.AmazonInfo.Builder
				dynamic.setServerListImpl(new DomainExtractingServerList(list, dynamic
						.getClientConfig(), this.approximateZoneFromHostname));
			}
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
