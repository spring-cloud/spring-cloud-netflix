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
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
 * @author Gregor Zurowski
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

	Map<String, RouteDetails> invokeRouteDetails() {
		Map<String, RouteDetails> map = new LinkedHashMap<>();
		for (Route route : this.routes.getRoutes()) {
			map.put(route.getFullPath(), new RouteDetails(route));
		}
		return map;
	}

	/**
	 * Container for exposing Zuul {@link Route} details as JSON.
	 */
	@JsonPropertyOrder({ "id", "fullPath", "location" })
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public static class RouteDetails {

		private String id;

		private String fullPath;

		private String path;

		private String location;

		private String prefix;

		private Boolean retryable;

		private Set<String> sensitiveHeaders;

		private boolean customSensitiveHeaders;

		private boolean prefixStripped;

		public RouteDetails() {
		}

		RouteDetails(final Route route) {
			this.id = route.getId();
			this.fullPath = route.getFullPath();
			this.path = route.getPath();
			this.location = route.getLocation();
			this.prefix = route.getPrefix();
			this.retryable = route.getRetryable();
			this.sensitiveHeaders = route.getSensitiveHeaders();
			this.customSensitiveHeaders = route.isCustomSensitiveHeaders();
			this.prefixStripped = route.isPrefixStripped();
		}

		public String getId() {
			return id;
		}

		public String getFullPath() {
			return fullPath;
		}

		public String getPath() {
			return path;
		}

		public String getLocation() {
			return location;
		}

		public String getPrefix() {
			return prefix;
		}

		public Boolean getRetryable() {
			return retryable;
		}

		public Set<String> getSensitiveHeaders() {
			return sensitiveHeaders;
		}

		public boolean isCustomSensitiveHeaders() {
			return customSensitiveHeaders;
		}

		public boolean isPrefixStripped() {
			return prefixStripped;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			RouteDetails that = (RouteDetails) o;
			return Objects.equals(id, that.id) &&
					Objects.equals(fullPath, that.fullPath) &&
					Objects.equals(path, that.path) &&
					Objects.equals(location, that.location) &&
					Objects.equals(prefix, that.prefix) &&
					Objects.equals(retryable, that.retryable) &&
					Objects.equals(sensitiveHeaders, that.sensitiveHeaders) &&
					customSensitiveHeaders == that.customSensitiveHeaders &&
					prefixStripped == that.prefixStripped;
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, fullPath, path, location, prefix, retryable,
					sensitiveHeaders, customSensitiveHeaders, prefixStripped);
		}
	}

}
