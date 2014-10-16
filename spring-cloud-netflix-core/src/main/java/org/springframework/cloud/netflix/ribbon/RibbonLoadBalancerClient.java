package org.springframework.cloud.netflix.ribbon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import com.netflix.client.ClientFactory;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class RibbonLoadBalancerClient implements LoadBalancerClient {

	@Autowired
	private ServerListInitializer serverListInitializer;

	private Map<String, ILoadBalancer> balancers = new HashMap<String, ILoadBalancer>();

	public RibbonLoadBalancerClient(List<BaseLoadBalancer> balancers) {
		for (BaseLoadBalancer balancer : balancers) {
			this.balancers.put(balancer.getName(), balancer);
		}
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		serverListInitializer.initialize(serviceId);
		ILoadBalancer loadBalancer = this.balancers.get(serviceId);
		if (loadBalancer == null) {
			loadBalancer = ClientFactory.getNamedLoadBalancer(serviceId);
		}
		Server server = loadBalancer.chooseServer("default");
		if (server == null) {
			throw new IllegalStateException(
					"Unable to locate ILoadBalancer for service: " + serviceId);
		}
		return new RibbonServer(serviceId, server);
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
