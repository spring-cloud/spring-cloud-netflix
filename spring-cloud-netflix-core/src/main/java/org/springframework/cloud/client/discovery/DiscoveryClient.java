package org.springframework.cloud.client.discovery;

import org.springframework.cloud.client.ServiceInstance;

/**
 * @author Spencer Gibb
 */
public interface DiscoveryClient {
    /**
     * @return ServiceInstance with information used to register the local service
     */
    public ServiceInstance getLocalServiceInstance();
}
