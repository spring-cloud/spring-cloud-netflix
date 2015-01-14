package org.springframework.cloud.netflix.zuul;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;

/**
 * MVC HandlerMapping that maps incoming request paths to remote services.
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
public class ZuulHandlerMapping extends AbstractUrlHandlerMapping {

	private RouteLocator routeLocator;

	private ZuulController zuul;

	@Autowired
	public ZuulHandlerMapping(RouteLocator routeLocator, ZuulController zuul) {
		this.routeLocator = routeLocator;
		this.zuul = zuul;
		setOrder(-200);
	}

	protected void registerHandlers() {
		Collection<String> routes = this.routeLocator.getRoutePaths();
		if (routes.isEmpty()) {
			this.logger.warn("No routes found from ProxyRouteLocator");
		}
		else {
			for (String url : routes) {
				registerHandler(url, this.zuul);
			}
		}
	}

}
