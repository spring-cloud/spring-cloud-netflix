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

package org.springframework.cloud.netflix.zuul.filters;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Data
@ConfigurationProperties("zuul")
public class ZuulProperties {

	/**
	 *
	 */
	private static final List<String> SECURITY_HEADERS = Arrays.asList("Pragma",
			"Cache-Control", "X-Frame-Options", "X-Content-Type-Options",
			"X-XSS-Protection", "Expires");

	private String prefix = "";

	private boolean stripPrefix = true;

	private Boolean retryable;

	private Map<String, ZuulRoute> routes = new LinkedHashMap<>();

	private boolean addProxyHeaders = true;

	private Set<String> ignoredServices = new LinkedHashSet<>();

	private Set<String> ignoredPatterns = new LinkedHashSet<>();

	private Set<String> ignoredHeaders = new LinkedHashSet<>();

	private String servletPath = "/zuul";

	private boolean ignoreLocalService = true;

	private Host host = new Host();

	private boolean traceRequestBody = true;

	private boolean removeSemicolonContent = true;		

	public Set<String> getIgnoredHeaders() {
		Set<String> ignoredHeaders = new LinkedHashSet<>(this.ignoredHeaders);
		if (ClassUtils.isPresent(
				"org.springframework.security.config.annotation.web.WebSecurityConfigurer",
				null) && Collections.disjoint(ignoredHeaders, SECURITY_HEADERS)) {
			// Allow Spring Security in the gateway to control these headers
			ignoredHeaders.addAll(SECURITY_HEADERS);
		}
		return ignoredHeaders;
	}

	public void setIgnoredHeaders(Set<String> ignoredHeaders) {
		this.ignoredHeaders.addAll(ignoredHeaders);
	}

	@PostConstruct
	public void init() {
		for (Entry<String, ZuulRoute> entry : this.routes.entrySet()) {
			ZuulRoute value = entry.getValue();
			if (!StringUtils.hasText(value.getLocation())) {
				value.serviceId = entry.getKey();
			}
			if (!StringUtils.hasText(value.getId())) {
				value.id = entry.getKey();
			}
			if (!StringUtils.hasText(value.getPath())) {
				value.path = "/" + entry.getKey() + "/**";
			}
		}
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ZuulRoute {

		private String id;

		private String path;

		private String serviceId;

		private String url;

		private boolean stripPrefix = true;

		private Boolean retryable;

		private Set<String> sensitiveHeaders = new LinkedHashSet<>(
				Arrays.asList("Cookie", "Set-Cookie", "Authorization"));

		public ZuulRoute(String text) {
			String location = null;
			String path = text;
			if (text.contains("=")) {
				String[] values = StringUtils
						.trimArrayElements(StringUtils.split(text, "="));
				location = values[1];
				path = values[0];
			}
			this.id = extractId(path);
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			setLocation(location);
			this.path = path;
		}

		public ZuulRoute(String path, String location) {
			this.id = extractId(path);
			this.path = path;
			setLocation(location);
		}

		public String getLocation() {
			if (StringUtils.hasText(this.url)) {
				return this.url;
			}
			return this.serviceId;
		}

		public void setLocation(String location) {
			if (location != null
					&& (location.startsWith("http:") || location.startsWith("https:"))) {
				this.url = location;
			}
			else {
				this.serviceId = location;
			}
		}

		private String extractId(String path) {
			path = path.startsWith("/") ? path.substring(1) : path;
			path = path.replace("/*", "").replace("*", "");
			return path;
		}

		public Route getRoute(String prefix) {
			return new Route(this.id, this.path, getLocation(), prefix, this.retryable,
					this.sensitiveHeaders);
		}

	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Host {
		private int maxTotalConnections = 200;
		private int maxPerRouteConnections = 20;
	}

	public String getServletPattern() {
		String path = this.servletPath;
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		if (!path.contains("*")) {
			path = path.endsWith("/") ? (path + "*") : (path + "/*");
		}
		return path;
	}

}
