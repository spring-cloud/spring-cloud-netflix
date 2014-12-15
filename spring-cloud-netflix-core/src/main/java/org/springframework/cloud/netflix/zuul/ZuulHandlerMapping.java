package org.springframework.cloud.netflix.zuul;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryHeartbeatEvent;
import org.springframework.cloud.client.discovery.InstanceRegisteredEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MVC HandlerMapping that maps incoming request paths to remote services.
 * 
 * @author Spencer Gibb
 * @author Dave Syer
 */
@ManagedResource(description = "Can be used to list and reset the reverse proxy routes")
public class ZuulHandlerMapping extends AbstractUrlHandlerMapping implements
		ApplicationListener<ApplicationEvent> {

	private ProxyRouteLocator routeLocator;

	private ZuulController zuul;

	private AtomicReference<Object> latestHeartbeat = new AtomicReference<>();

	@Autowired
	public ZuulHandlerMapping(ProxyRouteLocator routeLocator, ZuulController zuul) {
		this.routeLocator = routeLocator;
		this.zuul = zuul;
		setOrder(-200);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof InstanceRegisteredEvent
				|| event instanceof RefreshScopeRefreshedEvent) {
			reset();
		} else if (event instanceof DiscoveryHeartbeatEvent) {
			DiscoveryHeartbeatEvent e = (DiscoveryHeartbeatEvent) event;
			if (latestHeartbeat.get() == null || !latestHeartbeat.get().equals(e.getValue())) {
				latestHeartbeat.set(e.getValue());
				reset();
			}
		}
	}

	protected void registerHandlers() {
        Collection<String> routes = routeLocator.getRoutePaths();
		if (routes.isEmpty()) {
			logger.warn("No routes found from ProxyRouteLocator");
		}
		else {
			for (String url : routes) {
				registerHandler(url, zuul);
			}
		}
	}

	@ManagedOperation
	public Map<String, String> reset() {
		routeLocator.resetRoutes();
		registerHandlers();
		return getRoutes();
	}

	@ManagedAttribute
	public Map<String, String> getRoutes() {
		return routeLocator.getRoutes();
	}

}
