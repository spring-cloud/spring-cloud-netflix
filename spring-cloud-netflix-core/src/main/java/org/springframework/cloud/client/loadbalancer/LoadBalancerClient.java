package org.springframework.cloud.client.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;

/**
 * @author Spencer Gibb
 */
public interface LoadBalancerClient {
    /**
     * Choose a {@see ServiceInstance} from the LoadBalancer for the specified service
     * @param serviceId the service id to look up the LoadBalancer
     * @return a ServiceInstance that matches the serviceId
     */
    public ServiceInstance choose(String serviceId);

    /**
     * Choose a {@see ServiceInstance} from the LoadBalancer for the specified service
     * @param serviceId the service id to look up the LoadBalancer
     * @param request allows implementations to execute pre and post actions such as incrementing metrics
     * @return the result of the LoadBalancerRequest callback on the selected ServiceInstance
     */
    public <T> T choose(String serviceId, LoadBalancerRequest<T> request);
}
