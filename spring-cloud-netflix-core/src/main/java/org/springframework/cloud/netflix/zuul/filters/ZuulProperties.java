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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Data
@ConfigurationProperties("zuul")
public class ZuulProperties {

	/**
	 * Headers that are generally expected to be added by Spring Security, and hence often
	 * duplicated if the proxy and the backend are secured with Spring. By default they
	 * are added to the ignored headers if Spring Security is present.
	 */
	public static final List<String> SECURITY_HEADERS = Arrays.asList("Pragma",
			"Cache-Control", "X-Frame-Options", "X-Content-Type-Options",
			"X-XSS-Protection", "Expires");

	/**
	 * A common prefix for all routes.
	 */
	private String prefix = "";

	/**
	 * Flag saying whether to strip the prefix from the path before forwarding.
	 */
	private boolean stripPrefix = true;

	/**
	 * Flag for whether retry is supported by default (assuming the routes themselves
	 * support it).
	 */
	private Boolean retryable;

	/**
	 * Map of route names to properties.
	 */
	private Map<String, ZuulRoute> routes = new LinkedHashMap<>();

	/**
	 * Flag to determine whether the proxy adds X-Forwarded-* headers.
	 */
	private boolean addProxyHeaders = true;

	/**
	 * Flag to determine whether the proxy forwards the Host header.
	 */
	private boolean addHostHeader = false;

	/**
	 * Set of service names not to consider for proxying automatically. By default all
	 * services in the discovery client will be proxied.
	 */
	private Set<String> ignoredServices = new LinkedHashSet<>();

	private Set<String> ignoredPatterns = new LinkedHashSet<>();

	/**
	 * Names of HTTP headers to ignore completely (i.e. leave them out of downstream
	 * requests and drop them from downstream responses).
	 */
	private Set<String> ignoredHeaders = new LinkedHashSet<>();

	/**
	 * Path to install Zuul as a servlet (not part of Spring MVC). The servlet is more
	 * memory efficient for requests with large bodies, e.g. file uploads.
	 */
	private String servletPath = "/zuul";

	private boolean ignoreLocalService = true;

	/**
	 * Host properties controlling default connection pool properties.
	 */
	private Host host = new Host();

	/**
	 * Flag to say that request bodies can be traced.
	 */
	private boolean traceRequestBody = true;

	/**
	 * Flag to say that path elements past the first semicolon can be dropped.
	 */
	private boolean removeSemicolonContent = true;

	/**
	 * List of sensitive headers that are not passed to downstream requests. Defaults to a
	 * "safe" set of headers that commonly contain user credentials. It's OK to remove
	 * those from the list if the downstream service is part of the same system as the
	 * proxy, so they are sharing authentication data. If using a physical URL outside
	 * your own domain, then generally it would be a bad idea to leak user credentials.
	 */
	private Set<String> sensitiveHeaders = new LinkedHashSet<>(
			Arrays.asList("Cookie", "Set-Cookie", "Authorization"));

	/**
	 * Flag to say whether the hostname for ssl connections should be verified or not. Default is true.
	 * This should only be used in test setups!
	 */
	private boolean sslHostnameValidationEnabled =true;

	private ExecutionIsolationStrategy ribbonIsolationStrategy = SEMAPHORE;
	
	private HystrixSemaphore semaphore = new HystrixSemaphore();
	
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
	@NoArgsConstructor
	public static class ZuulRoute {

		/**
		 * The ID of the route (the same as its map key by default).
		 */
		private String id;

		/**
		 * The path (pattern) for the route, e.g. /foo/**.
		 */
		private String path;

		/**
		 * The service ID (if any) to map to this route. You can specify a physical URL or
		 * a service, but not both.
		 */
		private String serviceId;

		/**
		 * A full physical URL to map to the route. An alternative is to use a service ID
		 * and service discovery to find the physical address.
		 */
		private String url;

		/**
		 * Flag to determine whether the prefix for this route (the path, minus pattern
		 * patcher) should be stripped before forwarding.
		 */
		private boolean stripPrefix = true;

		/**
		 * Flag to indicate that this route should be retryable (if supported). Generally
		 * retry requires a service ID and ribbon.
		 */
		private Boolean retryable;

		/**
		 * List of sensitive headers that are not passed to downstream requests. Defaults
		 * to a "safe" set of headers that commonly contain user credentials. It's OK to
		 * remove those from the list if the downstream service is part of the same system
		 * as the proxy, so they are sharing authentication data. If using a physical URL
		 * outside your own domain, then generally it would be a bad idea to leak user
		 * credentials.
		 */
		private Set<String> sensitiveHeaders = new LinkedHashSet<>();

		private boolean customSensitiveHeaders = false;

		public ZuulRoute(String id, String path, String serviceId, String url,
				boolean stripPrefix, Boolean retryable, Set<String> sensitiveHeaders) {
			this.id = id;
			this.path = path;
			this.serviceId = serviceId;
			this.url = url;
			this.stripPrefix = stripPrefix;
			this.retryable = retryable;
			this.sensitiveHeaders = sensitiveHeaders;
		}

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
					isCustomSensitiveHeaders() ? this.sensitiveHeaders : null);
		}

		public void setSensitiveHeaders(Set<String> headers) {
			this.customSensitiveHeaders = true;
			this.sensitiveHeaders = new LinkedHashSet<>(headers);
		}

		public boolean isCustomSensitiveHeaders() {
			return this.customSensitiveHeaders;
		}

	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Host {
		/**
		 * The maximum number of total connections the proxy can hold open to backends.
		 */
		private int maxTotalConnections = 200;
		/**
		 * The maximum number of connections that can be used by a single route.
		 */
		private int maxPerRouteConnections = 20;
	}
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class HystrixSemaphore {
		/**
		 * The maximum number of total semaphores for Hystrix.
		 */
		private int maxSemaphores = 100;
		
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
