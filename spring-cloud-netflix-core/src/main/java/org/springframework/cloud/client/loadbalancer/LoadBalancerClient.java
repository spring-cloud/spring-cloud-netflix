package org.springframework.cloud.client.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;

/**
 * @author Spencer Gibb
 */
public interface LoadBalancerClient {
    /**
     * Choose a {@see ServiceInstance} from the LoadBalancer for the specified service
     * @param serviceId The serviceId to use to look up the LoadBalancer
     * @return
     */
    public ServiceInstance choose(String serviceId);
}
