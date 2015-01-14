package org.springframework.cloud.netflix.zuul;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Endpoint to display and reset the zuul proxy routes
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
@ManagedResource(description = "Can be used to list and reset the reverse proxy routes")
public class RoutesEndpoint implements MvcEndpoint, ApplicationEventPublisherAware {

	private ProxyRouteLocator routes;
	private ApplicationEventPublisher publisher;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Autowired
	public RoutesEndpoint(ProxyRouteLocator routes) {
		this.routes = routes;
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	@ManagedOperation
	public Map<String, String> reset() {
		this.publisher.publishEvent(new RoutesRefreshedEvent(this.routes));
		return getRoutes();
	}

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	@ManagedAttribute
	public Map<String, String> getRoutes() {
		return this.routes.getRoutes();
	}

	@Override
	public String getPath() {
		return "/routes";
	}

	@Override
	public boolean isSensitive() {
		return true;
	}

	@Override
	public Class<? extends Endpoint<?>> getEndpointType() {
		return null;
	}

}
