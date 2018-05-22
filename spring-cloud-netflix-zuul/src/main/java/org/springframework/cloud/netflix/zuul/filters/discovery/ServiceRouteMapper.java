package org.springframework.cloud.netflix.zuul.filters.discovery;

/**
 * Provide a way to apply convention between routes and discovered services name.
 *
 * @author Stéphane LEROY
 *
 */
public interface ServiceRouteMapper {

	/**
	 * Take a service Id (its discovered name) and return a route path.
	 *
	 * @param serviceId service discovered name
	 * @return route path
	 */
	String apply(String serviceId);
}
