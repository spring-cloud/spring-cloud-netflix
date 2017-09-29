package org.springframework.cloud.netflix.zuul.filters.discovery;

/**
 * @author Stéphane Leroy
 *
 * A simple passthru service route mapper.
 */
public class SimpleServiceRouteMapper implements ServiceRouteMapper {
	@Override
	public String apply(String serviceId) {
		return serviceId;
	}
}
