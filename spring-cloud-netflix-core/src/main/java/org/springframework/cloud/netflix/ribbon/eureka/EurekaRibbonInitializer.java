package org.springframework.cloud.netflix.ribbon.eureka;

import com.netflix.config.ConfigurationManager;
import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList;
import org.springframework.cloud.netflix.ribbon.ServerListInitializer;

/**
 * @author Spencer Gibb
 */
public class EurekaRibbonInitializer implements ServerListInitializer {

    @Override
    public void initialize(String serviceId) {
        //TODO: should this look more like hibernate spring boot props?
        setProp(serviceId, "NIWSServerListClassName", DiscoveryEnabledNIWSServerList.class.getName());
        setProp(serviceId, "DeploymentContextBasedVipAddresses", serviceId); //FIXME: what should this be?
    }

    protected void setProp(String serviceId, String suffix, String value) {
        //how to set the namespace properly?
        String namespace = "ribbon";
        ConfigurationManager.getConfigInstance().setProperty(serviceId + "."+ namespace +"." + suffix, value);
    }
}
