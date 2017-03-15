/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
