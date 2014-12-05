package org.springframework.cloud.netflix.ribbon;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;

import com.google.common.base.Throwables;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerStats;
import com.netflix.servo.monitor.Stopwatch;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class RibbonLoadBalancerClient implements LoadBalancerClient {

    private SpringClientFactory clientFactory;

	public RibbonLoadBalancerClient(SpringClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

    @Override
    public URI reconstructURI(ServiceInstance instance, URI original) {
        String serviceId = instance.getServiceId();
        RibbonLoadBalancerContext context = clientFactory.getLoadBalancerContext(serviceId);
        Server server = new Server(instance.getHost(), instance.getPort());
        return context.reconstructURIWithServer(server, original);
    }

    @Override
	public ServiceInstance choose(String serviceId) {
		return new RibbonServer(serviceId, getServer(serviceId));
	}

    @Override
    public <T> T execute(String serviceId, LoadBalancerRequest<T> request) {
        ILoadBalancer loadBalancer = getLoadBalancer(serviceId);
        RibbonLoadBalancerContext context = clientFactory.getLoadBalancerContext(serviceId);
        Server server = getServer(serviceId, loadBalancer);
        RibbonServer ribbonServer = new RibbonServer(serviceId, server);

        ServerStats serverStats = context.getServerStats(server);
        context.noteOpenConnection(serverStats);
        Stopwatch tracer = context.getExecuteTracer().start();

        try {

            T returnVal = request.apply(ribbonServer);
            recordStats(context, tracer, serverStats, returnVal, null);
            return returnVal;
        } catch (Exception e) {
            recordStats(context, tracer, serverStats, null, e);
            Throwables.propagate(e);
        }
        return null;
    }

    private void recordStats(RibbonLoadBalancerContext context, Stopwatch tracer, ServerStats serverStats, Object entity, Throwable exception) {
        tracer.stop();
        long duration = tracer.getDuration(TimeUnit.MILLISECONDS);
        context.noteRequestCompletion(serverStats, entity, exception, duration, null/*errorHandler*/);
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
    	return clientFactory.getLoadBalancer(serviceId);
    }

    protected static class RibbonServer implements ServiceInstance {
		protected String serviceId;
		protected Server server;

		protected RibbonServer(String serviceId, Server server) {
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
		public int getPort() {
			return server.getPort();
		}
	}
}
