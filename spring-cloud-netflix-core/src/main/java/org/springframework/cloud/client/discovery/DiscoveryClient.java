package org.springframework.cloud.client.discovery;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * @author Spencer Gibb
 */
public interface DiscoveryClient {
    /**
     * @return ServiceInstance with information used to register the local service
     */
    public ServiceInstance getLocalServiceInstance();

    public List<ServiceInstance> getInstances(String serviceId);
}
