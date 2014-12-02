package org.springframework.cloud.netflix.zuul;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.netflix.zuul.ZuulProperties.ZuulRoute;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.util.ReflectionUtils;

/**
 * @author Spencer Gibb
 */
@Slf4j
public class ZuulRouteLocator implements ApplicationListener<EnvironmentChangeEvent> {

	public static final String DEFAULT_ROUTE = "/";

	private DiscoveryClient discovery;

	private ZuulProperties properties;

	private Field propertySourcesField;
	private AtomicReference<LinkedHashMap<String, String>> routes = new AtomicReference<>();

	public ZuulRouteLocator(DiscoveryClient discovery, ZuulProperties properties) {
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
			if (key.startsWith("zuul.route")) {
				resetRoutes();
				return;
			}
		}
	}

	public Collection<String> getRoutePaths() {
		return getRoutes().keySet();
	}

	public Map<String, String> getRoutes() {
		if (routes.get() == null) {
			resetRoutes();
		}

		return routes.get();
	}

    //access so ZuulHandlerMapping actuator can reset it's mappings
    /*package*/ void resetRoutes() {
        LinkedHashMap<String, String> newValue = locateRoutes();
        routes.set(newValue);
    }

    protected LinkedHashMap<String, String> locateRoutes() {
		LinkedHashMap<String, String> routesMap = new LinkedHashMap<>();

		addConfiguredRoutes(routesMap);

		String defaultServiceId = routesMap.get(DEFAULT_ROUTE);

		// Add routes for discovery services by default
		List<String> services = discovery.getServices();
		for (String serviceId : services) {
			// Ignore specifically ignored services and those that were manually configured
			String key = "/" + serviceId + "/**";
			if (!properties.getIgnoredServices().contains(serviceId) && !routesMap.containsKey(key)) {
				routesMap.put(key, serviceId);
			}
		}

		if (defaultServiceId != null) {
			// move the defaultServiceId to the end
			routesMap.remove(DEFAULT_ROUTE);
			routesMap.put(DEFAULT_ROUTE, defaultServiceId);
		}
		return routesMap;
	}

	protected void addConfiguredRoutes(Map<String, String> routes) {
		Map<String, ZuulRoute> routeEntries = properties.getRoutesWithDefaultServiceIds();
		for (ZuulRoute entry : routeEntries.values()) {
			String location = entry.getLocation();
			String route = entry.getPath();

			if (routes.containsKey(route)) {
				log.warn("Overwriting route {}: already defined by {}", route,
						routes.get(route));
			}
			routes.put(route, location);
		}
	}

}
