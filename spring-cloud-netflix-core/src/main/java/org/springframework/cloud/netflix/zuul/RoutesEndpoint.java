/*
 * Copyright 2013-2015 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Endpoint to display the zuul proxy routes
 *
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Ryan Baxter
 */
@ManagedResource(description = "Can be used to list the reverse proxy routes")
@ConfigurationProperties(prefix = "endpoints.routes")
public class RoutesEndpoint extends AbstractEndpoint<Map<String, String>> {

	private static final String ID = "routes";

	private RouteLocator routes;

	private ApplicationEventPublisher publisher;

	@Autowired
	public RoutesEndpoint(RouteLocator routes) {
		super(ID, true);
		this.routes = routes;
	}

	@ManagedAttribute
	public Map<String, String> invoke() {
		Map<String, String> map = new LinkedHashMap<>();
		for (Route route : this.routes.getRoutes()) {
			map.put(route.getFullPath(), route.getLocation());
		}
		return map;
	}
}
