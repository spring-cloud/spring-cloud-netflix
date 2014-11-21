package org.springframework.cloud.netflix.ribbon.eureka;

import static com.netflix.client.config.CommonClientConfigKey.DeploymentContextBasedVipAddresses;
import static com.netflix.client.config.CommonClientConfigKey.EnableZoneAffinity;
import static com.netflix.client.config.CommonClientConfigKey.NFLoadBalancerRuleClassName;
import static com.netflix.client.config.CommonClientConfigKey.NIWSServerListClassName;
import static com.netflix.client.config.CommonClientConfigKey.NIWSServerListFilterClassName;

import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import org.springframework.cloud.netflix.ribbon.RibbonClientPreprocessor;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext.ContextKey;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList;

/**
 * Preprocessor that configures defaults for eureka-discovered ribbon clients.
 * Such as: <code>@zone</code>, NIWSServerListClassName, DeploymentContextBasedVipAddresses,
 *   NFLoadBalancerRuleClassName, NIWSServerListFilterClassName and more
 * 
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class EurekaRibbonClientPreprocessor implements RibbonClientPreprocessor {

    protected static final String VALUE_NOT_SET = "__not__set__";
    protected static final String DEFAULT_NAMESPACE = "ribbon";

    private EurekaClientConfig clientConfig;
    private SpringClientFactory clientFactory;

    public EurekaRibbonClientPreprocessor(EurekaClientConfig clientConfig, SpringClientFactory clientFactory) {
		this.clientConfig = clientConfig;
        this.clientFactory = clientFactory;
    }

	@Override
	public void preprocess(String serviceId) {
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
		// TODO: only set the property if it hasn't already been set?
		setProp(serviceId, NIWSServerListClassName.key(),
				DiscoveryEnabledNIWSServerList.class.getName());
		// FIXME: what should this be?
		setProp(serviceId, DeploymentContextBasedVipAddresses.key(), serviceId);
		setProp(serviceId, NFLoadBalancerRuleClassName.key(),
				ZoneAvoidanceRule.class.getName());
		// TODO: use bean name indirection to get this filter to be a @Bean
		setProp(serviceId, NIWSServerListFilterClassName.key(),
				ZonePreferenceServerListFilter.class.getName());
		setProp(serviceId, EnableZoneAffinity.key(), "true");
		ILoadBalancer loadBalancer = clientFactory.getNamedLoadBalancer(serviceId);
		wrapServerList(loadBalancer);
	}

	private void wrapServerList(ILoadBalancer balancer) {
		if (balancer instanceof DynamicServerListLoadBalancer) {
			@SuppressWarnings("unchecked")
			DynamicServerListLoadBalancer<Server> dynamic = (DynamicServerListLoadBalancer<Server>) balancer;
			ServerList<Server> list = dynamic.getServerListImpl();
			if (!(list instanceof DomainExtractingServerList)) {
				// This is optional: you can use the native Eureka AWS features as long as
				// the server zone is populated. TODO: find a way to back off if AWS
				// metadata *is* available.
                // @see com.netflix.appinfo.AmazonInfo.Builder
				dynamic.setServerListImpl(new DomainExtractingServerList(list, dynamic.getClientConfig()));
			}
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
