package org.springframework.cloud.client.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;

/**
 * @author Spencer Gibb
 */
public interface LoadBalancerRequest<T> {
    public T apply(ServiceInstance instance);
}
