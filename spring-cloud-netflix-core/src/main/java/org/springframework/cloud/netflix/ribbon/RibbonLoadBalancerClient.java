package org.springframework.cloud.netflix.ribbon;

import com.google.common.base.Throwables;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerStats;
import com.netflix.servo.monitor.Stopwatch;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class RibbonLoadBalancerClient implements LoadBalancerClient {

	private RibbonClientPreprocessor ribbonClientPreprocessor;

    private SpringClientFactory clientFactory;

	private Map<String, ILoadBalancer> balancers = new ConcurrentHashMap<>();
    private Map<String, RibbonLoadBalancerContext> contexts = new ConcurrentHashMap<>();

	public RibbonLoadBalancerClient(RibbonClientPreprocessor ribbonClientPreprocessor, SpringClientFactory clientFactory, List<BaseLoadBalancer> balancers) {
		this.ribbonClientPreprocessor = ribbonClientPreprocessor;
		this.clientFactory = clientFactory;
		for (BaseLoadBalancer balancer : balancers) {
			this.balancers.put(balancer.getName(), balancer);
		}
	}

    @Override
    public URI reconstructURI(ServiceInstance instance, URI original) {
        String serviceId = instance.getServiceId();
        RibbonLoadBalancerContext context = getOrCreateLoadBalancerContext(serviceId, getLoadBalancer(serviceId));
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
        RibbonLoadBalancerContext context = getOrCreateLoadBalancerContext(serviceId, loadBalancer);
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

    protected RibbonLoadBalancerContext getOrCreateLoadBalancerContext(String serviceId, ILoadBalancer loadBalancer) {
        RibbonLoadBalancerContext context = contexts.get(serviceId);
        if (context == null) {
            context = new RibbonLoadBalancerContext(loadBalancer);
            contexts.put(serviceId, context);
        }
        return context;
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
