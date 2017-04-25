package org.springframework.cloud.netflix.zuul.filters.discovery;

import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;

/**
 * @author Stéphane Leroy
 * @author Saiyed Zaidi
 *
 * A simple passthru service route mapper.
 */
public class SimpleServiceRouteMapper implements ServiceRouteMapper {
	@Override
	public String apply(String serviceId) {
		return serviceId;
	}
	
	@Override
	public ZuulRoute applyRoute(String serviceId) {
		return new ZuulRoute(serviceId, serviceId);
	}
}
