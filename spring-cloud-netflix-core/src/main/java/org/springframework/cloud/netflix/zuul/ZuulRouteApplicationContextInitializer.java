package org.springframework.cloud.netflix.zuul;

import org.springframework.cloud.netflix.ribbon.RibbonApplicationContextInitializer;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Responsible for taking in the list of registered serviceid's (Ribbon client names)
 * and creating the Spring {@link org.springframework.context.ApplicationContext} on
 * start-up
 *
 * @author Biju Kunjummen
 */

public class ZuulRouteApplicationContextInitializer extends
		RibbonApplicationContextInitializer {
	public ZuulRouteApplicationContextInitializer(SpringClientFactory springClientFactory,
			ZuulProperties zuulProperties) {
		super(springClientFactory, getServiceIdsFromZuulProps(zuulProperties));
	}

	private static List<String> getServiceIdsFromZuulProps(ZuulProperties zuulProperties) {
		Map<String, ZuulProperties.ZuulRoute> zuulRoutes = zuulProperties.getRoutes();
		Collection<ZuulProperties.ZuulRoute> registeredRoutes = zuulRoutes.values();
		List<String> serviceIds = new ArrayList<>();
		if (registeredRoutes != null) {
			for (ZuulProperties.ZuulRoute route: registeredRoutes) {
				String serviceId = route.getServiceId();
				if (serviceId != null) {
					serviceIds.add(serviceId);
				}
			}
		}
		return serviceIds;
	}
}
