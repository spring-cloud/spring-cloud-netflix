package org.springframework.cloud.netflix.zuul.filters.discovery;

import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;

/**
 * Provide a way to apply convention between routes and discovered services name.
 *
 * @author Stéphane LEROY
 * @author Saiyed Zaidi
 *
 */
public interface ServiceRouteMapper {

	/**
	 * Take a service Id (its discovered name) and return a route path.
	 *
	 * @param serviceId
	 *            service discovered name
	 * @deprecated Replaced by the {@link #applyRoute(String) applyRoute}
	 *             method.
	 * @return route path
	 */
	String apply(String serviceId);

	/**
	 * Take a service Id (its discovered name) and return a route.
	 *
	 * @param serviceId
	 *            service discovered name
	 * @return route
	 */
	ZuulRoute applyRoute(String serviceId);
}
