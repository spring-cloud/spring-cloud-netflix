package org.springframework.cloud.netflix.ribbon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;

import com.netflix.loadbalancer.AbstractLoadBalancer;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class RibbonLoadBalancerClient implements LoadBalancerClient {

	private RibbonClientPreprocessor ribbonClientPreprocessor;

    private SpringClientFactory clientFactory;

	private Map<String, ILoadBalancer> balancers = new HashMap<String, ILoadBalancer>();

	public RibbonLoadBalancerClient(RibbonClientPreprocessor ribbonClientPreprocessor, SpringClientFactory clientFactory, List<BaseLoadBalancer> balancers) {
		this.ribbonClientPreprocessor = ribbonClientPreprocessor;
		this.clientFactory = clientFactory;
		for (BaseLoadBalancer balancer : balancers) {
			this.balancers.put(balancer.getName(), balancer);
		}
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		return new RibbonServer(serviceId, getServer(serviceId));
	}

    @Override
    public <T> T choose(String serviceId, LoadBalancerRequest<T> request) {
        ILoadBalancer loadBalancer = getLoadBalancer(serviceId);
        Server server = getServer(serviceId, loadBalancer);
        RibbonServer ribbonServer = new RibbonServer(serviceId, server);
        try {
            return request.apply(ribbonServer);
        } finally {
            if (loadBalancer instanceof AbstractLoadBalancer) {
                AbstractLoadBalancer.class.cast(loadBalancer).getLoadBalancerStats().incrementNumRequests(server);
            }
        }
    }

    protected Server getServer(String serviceId) {
        return getServer(serviceId, getLoadBalancer(serviceId));
    }
    protected Server getServer(String serviceId, ILoadBalancer loadBalancer) {
        Server server = loadBalancer.chooseServer("default");
        if (server == null) {
            throw new IllegalStateException(
                    "Unable to locate ILoadBalancer for service: " + serviceId);
        }
        return server;
    }

    protected ILoadBalancer getLoadBalancer(String serviceId) {
        ribbonClientPreprocessor.preprocess(serviceId);
        ILoadBalancer loadBalancer = this.balancers.get(serviceId);
        if (loadBalancer == null) {
            loadBalancer = clientFactory.getNamedLoadBalancer(serviceId);
        }
        return loadBalancer;
    }

    private class RibbonServer implements ServiceInstance {
		private String serviceId;
		private Server server;

		private RibbonServer(String serviceId, Server server) {
			this.serviceId = serviceId;
			this.server = server;
		}

		@Override
		public String getServiceId() {
			return serviceId;
		}

		@Override
		public String getHost() {
			return server.getHost();
		}

		@Override
		public String getIpAddress() {
			return null; // TODO: ribbon doesn't supply ip
		}

		@Override
		public int getPort() {
			return server.getPort();
		}
	}
}
