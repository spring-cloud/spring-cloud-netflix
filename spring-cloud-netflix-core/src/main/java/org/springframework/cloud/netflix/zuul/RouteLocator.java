package org.springframework.cloud.netflix.zuul;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.util.ReflectionUtils;

/**
 * @author Spencer Gibb
 */
@Slf4j
public class RouteLocator implements ApplicationListener<EnvironmentChangeEvent> {

	public static final String DEFAULT_ROUTE = "/";

	private DiscoveryClient discovery;

	private ZuulProperties properties;

	private Field propertySourcesField;
	private AtomicReference<LinkedHashMap<String, String>> routes = new AtomicReference<>();

	public RouteLocator(DiscoveryClient discovery, ZuulProperties properties) {
		this.discovery = discovery;
		this.properties = properties;
		initField();
	}

	private void initField() {
		propertySourcesField = ReflectionUtils.findField(CompositePropertySource.class,
				"propertySources");
		propertySourcesField.setAccessible(true);
	}

	@Override
	public void onApplicationEvent(EnvironmentChangeEvent event) {
		for (String key : event.getKeys()) {
			if (key.startsWith(properties.getMapping())) {
				routes.set(locateRoutes());
				return;
			}
		}
	}

	public Map<String, String> getRoutes() {
		if (routes.get() == null) {
			routes.set(locateRoutes());
		}

		return routes.get();
	}

	protected LinkedHashMap<String, String> locateRoutes() {
		LinkedHashMap<String, String> routesMap = new LinkedHashMap<>();

		// Add routes for discovery services by default
		List<String> services = discovery.getServices();
		for (String serviceId : services) {
			// Ignore specified services
			if (!properties.getIgnoredServices().contains(serviceId))
				routesMap.put("/" + serviceId + "/**", serviceId);
		}

		addConfiguredRoutes(routesMap);

		String defaultServiceId = routesMap.get(DEFAULT_ROUTE);

		if (defaultServiceId != null) {
			// move the defaultServiceId to the end
			routesMap.remove(DEFAULT_ROUTE);
			routesMap.put(DEFAULT_ROUTE, defaultServiceId);
		}
		return routesMap;
	}

	protected void addConfiguredRoutes(Map<String, String> routes) {
		Map<String, String> routeEntries = properties.getRoute();
		for (Map.Entry<String, String> entry : routeEntries.entrySet()) {
			String serviceId = entry.getKey();
			String route = entry.getValue()	;

			if (routes.containsKey(route)) {
				log.warn("Overwriting route {}: already defined by {}", route,
						routes.get(route));
			}
			routes.put(route, serviceId);
		}
	}
}
