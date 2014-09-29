package org.springframework.cloud.netflix.eureka;

import com.netflix.config.ConfigurationManager;
import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList;
import org.springframework.cloud.client.ClientProperties;

/**
 * @author Spencer Gibb
 */
public class EurekaRibbonInitializer {

    public EurekaRibbonInitializer(ClientProperties clientProperties) {
        if (clientProperties.getServiceIds() != null) {
            for (String serviceId : clientProperties.getServiceIds()) {
                setServiceListClassAndVIP(serviceId);
            }
        }
    }

    public static void setServiceListClassAndVIP(String serviceId) {
        setProp(serviceId, "NIWSServerListClassName", DiscoveryEnabledNIWSServerList.class.getName());
        setProp(serviceId, "DeploymentContextBasedVipAddresses", serviceId); //FIXME: what should this be?
    }

    private static void setProp(String serviceId, String suffix, String value) {
        ConfigurationManager.getConfigInstance().setProperty(serviceId + ".ribbon." + suffix, value);
    }
}
