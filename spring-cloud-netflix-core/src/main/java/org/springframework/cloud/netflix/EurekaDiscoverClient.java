package org.springframework.cloud.netflix;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;

/**
 * @author Spencer Gibb
 */
public class EurekaDiscoverClient implements DiscoveryClient {

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
            public String getIpAddress() {
                return config.getIpAddress();
            }

            @Override
            public int getPort() {
                return config.getNonSecurePort();
            }
        };
    }
}
