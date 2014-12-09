package org.springframework.cloud.netflix.ribbon.eureka;

import static com.netflix.client.config.CommonClientConfigKey.DeploymentContextBasedVipAddresses;
import static com.netflix.client.config.CommonClientConfigKey.EnableZoneAffinity;
import static com.netflix.client.config.CommonClientConfigKey.NFLoadBalancerRuleClassName;
import static com.netflix.client.config.CommonClientConfigKey.NIWSServerListClassName;
import static com.netflix.client.config.CommonClientConfigKey.NIWSServerListFilterClassName;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext.ContextKey;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList;

/**
 * Preprocessor that configures defaults for eureka-discovered ribbon clients. Such as:
 * <code>@zone</code>, NIWSServerListClassName, DeploymentContextBasedVipAddresses,
 * NFLoadBalancerRuleClassName, NIWSServerListFilterClassName and more
 * 
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Configuration
public class EurekaRibbonClientConfiguration {

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
		if (clientConfig != null
				&& ConfigurationManager.getDeploymentContext().getValue(ContextKey.zone) == null) {
			String[] zones = clientConfig.getAvailabilityZones(clientConfig.getRegion());
			String zone = zones != null && zones.length > 0 ? zones[0] : null;
			if (zone != null) {
				// You can set this with archaius.deployment.* (maybe requires
				// custom deployment context)?
				ConfigurationManager.getDeploymentContext().setValue(ContextKey.zone,
						zone);
			}
		}
		// TODO: should this look more like hibernate spring boot props?
		setProp(serviceId, NIWSServerListClassName.key(),
				DiscoveryEnabledNIWSServerList.class.getName());
		// FIXME: what should this be?
		setProp(serviceId, DeploymentContextBasedVipAddresses.key(), serviceId);
		setProp(serviceId, NFLoadBalancerRuleClassName.key(),
				ZoneAvoidanceRule.class.getName());
		setProp(serviceId, NIWSServerListFilterClassName.key(),
				ZonePreferenceServerListFilter.class.getName());
		setProp(serviceId, EnableZoneAffinity.key(), "true");
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
