package org.springframework.cloud.netflix.eureka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * @author Spencer Gibb
 */
public class EurekaDiscoveryClient implements DiscoveryClient {

    @Autowired
    private EurekaInstanceConfigBean config;

    @Override
    public ServiceInstance getLocalServiceInstance() {
        return new ServiceInstance() {
            @Override
            public String getServiceId() {
                return config.getAppname();
            }

            @Override
            public String getHost() {
                return config.getHostname();
            }

            @Override
            public int getPort() {
                return config.getNonSecurePort();
            }
        };
    }
}
