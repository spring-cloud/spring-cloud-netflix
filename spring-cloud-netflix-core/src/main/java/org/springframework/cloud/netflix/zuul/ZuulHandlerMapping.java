package org.springframework.cloud.netflix.zuul;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.cloud.client.discovery.InstanceRegisteredEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;

/**
 * MVC HandlerMapping that maps incoming request paths to remote services.
 * 
 * @author Spencer Gibb
 * @author Dave Syer
 */
@ManagedResource(description = "Can be used to list and reset the reverse proxy routes")
public class ZuulHandlerMapping extends AbstractUrlHandlerMapping implements
		ApplicationListener<InstanceRegisteredEvent>, MvcEndpoint {

	private RouteLocator routeLocator;

	private ZuulController zuul;

	private ZuulProperties properties;

	@Autowired
	public ZuulHandlerMapping(RouteLocator routeLocator, ZuulController zuul,
			ZuulProperties properties) {
		this.routeLocator = routeLocator;
		this.zuul = zuul;
		this.properties = properties;
		setOrder(-200);
	}

	@Override
	public void onApplicationEvent(InstanceRegisteredEvent event) {
		registerHandlers(routeLocator.getRoutes());
	}

	private void registerHandlers(Map<String, String> routes) {
		if (routes.isEmpty()) {
			logger.warn("Neither 'urlMap' nor 'mappings' set on SimpleUrlHandlerMapping");
		}
		else {
			for (Map.Entry<String, String> entry : routes.entrySet()) {
				String url = entry.getKey();
				// Prepend with slash if not already present.
				if (!url.startsWith("/")) {
					url = "/" + url;
				}

				if (StringUtils.hasText(properties.getMapping())) {
					url = properties.getMapping() + url;
					if (!url.startsWith("/")) {
						url = "/" + url;
					}
				}

				registerHandler(url, zuul);
			}
		}
	}

	@RequestMapping(value = "routes", method = RequestMethod.POST)
	@ResponseBody
	@ManagedOperation
	public Map<String, String> reset() {
		Map<String, String> routes = routeLocator.getRoutes();
		registerHandlers(routes);
		return routes;
	}

	@RequestMapping(value = "routes", method = RequestMethod.GET)
	@ResponseBody
	@ManagedAttribute
	public Map<String, String> getRoutes() {
		return routeLocator.getRoutes();
	}

	@Override
	public String getPath() {
		return "/proxy";
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
