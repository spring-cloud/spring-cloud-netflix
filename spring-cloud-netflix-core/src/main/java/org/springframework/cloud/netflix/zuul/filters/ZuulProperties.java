/*
 * Copyright 2013-2017 the original author or authors.
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

import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 * @author Mathias Düsterhöft
 * @author Bilal Alp
 * @author Gregor Zurowski
 */
@ConfigurationProperties("zuul")
public class ZuulProperties {

	/**
	 * Headers that are generally expected to be added by Spring Security, and hence often
	 * duplicated if the proxy and the backend are secured with Spring. By default they
	 * are added to the ignored headers if Spring Security is present and ignoreSecurityHeaders = true.
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
	private Boolean retryable = false;

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
	 * Flag to say that SECURITY_HEADERS are added to ignored headers if spring security is on the classpath.
	 * By setting ignoreSecurityHeaders to false we can switch off this default behaviour. This should be used together with
	 * disabling the default spring security headers
	 * see https://docs.spring.io/spring-security/site/docs/current/reference/html/headers.html#default-security-headers
	 */
	private boolean ignoreSecurityHeaders = true;
	
	/**
	 * Flag to force the original query string encoding when building the backend URI in
	 * SimpleHostRoutingFilter. When activated, query string will be built using
	 * HttpServletRequest getQueryString() method instead of UriTemplate. Note that this
	 * flag is not used in RibbonRoutingFilter with services found via DiscoveryClient
	 * (like Eureka).
	 */
	private boolean forceOriginalQueryStringEncoding = false;

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

	private HystrixThreadPool threadPool = new HystrixThreadPool();
	
	public Set<String> getIgnoredHeaders() {
		Set<String> ignoredHeaders = new LinkedHashSet<>(this.ignoredHeaders);
		if (ClassUtils.isPresent(
				"org.springframework.security.config.annotation.web.WebSecurityConfigurer",
				null) && Collections.disjoint(ignoredHeaders, SECURITY_HEADERS) && ignoreSecurityHeaders) {
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

		public ZuulRoute() {}

		public ZuulRoute(String id, String path, String serviceId, String url,
				boolean stripPrefix, Boolean retryable, Set<String> sensitiveHeaders) {
			this.id = id;
			this.path = path;
			this.serviceId = serviceId;
			this.url = url;
			this.stripPrefix = stripPrefix;
			this.retryable = retryable;
			this.sensitiveHeaders = sensitiveHeaders;
			this.customSensitiveHeaders = sensitiveHeaders != null;
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
					isCustomSensitiveHeaders() ? this.sensitiveHeaders : null,
					this.stripPrefix);
		}

		public void setSensitiveHeaders(Set<String> headers) {
			this.customSensitiveHeaders = true;
			this.sensitiveHeaders = new LinkedHashSet<>(headers);
		}

		public boolean isCustomSensitiveHeaders() {
			return this.customSensitiveHeaders;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getServiceId() {
			return serviceId;
		}

		public void setServiceId(String serviceId) {
			this.serviceId = serviceId;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public boolean isStripPrefix() {
			return stripPrefix;
		}

		public void setStripPrefix(boolean stripPrefix) {
			this.stripPrefix = stripPrefix;
		}

		public Boolean getRetryable() {
			return retryable;
		}

		public void setRetryable(Boolean retryable) {
			this.retryable = retryable;
		}

		public Set<String> getSensitiveHeaders() {
			return sensitiveHeaders;
		}

		public void setCustomSensitiveHeaders(boolean customSensitiveHeaders) {
			this.customSensitiveHeaders = customSensitiveHeaders;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ZuulRoute that = (ZuulRoute) o;
			return customSensitiveHeaders == that.customSensitiveHeaders &&
					Objects.equals(id, that.id) &&
					Objects.equals(path, that.path) &&
					Objects.equals(retryable, that.retryable) &&
					Objects.equals(sensitiveHeaders, that.sensitiveHeaders) &&
					Objects.equals(serviceId, that.serviceId) &&
					stripPrefix == that.stripPrefix &&
					Objects.equals(url, that.url);
		}

		@Override
		public int hashCode() {
			return Objects.hash(customSensitiveHeaders, id, path, retryable,
					sensitiveHeaders, serviceId, stripPrefix, url);
		}

		@Override public String toString() {
			return new StringBuilder("ZuulRoute{").append("id='").append(id).append("', ")
					.append("path='").append(path).append("', ")
					.append("serviceId='").append(serviceId).append("', ")
					.append("url='").append(url).append("', ")
					.append("stripPrefix=").append(stripPrefix).append(", ")
					.append("retryable=").append(retryable).append(", ")
					.append("sensitiveHeaders=").append(sensitiveHeaders).append(", ")
					.append("customSensitiveHeaders=").append(customSensitiveHeaders).append(", ")
					.append("}").toString();
		}

	}

	public static class Host {
		/**
		 * The maximum number of total connections the proxy can hold open to backends.
		 */
		private int maxTotalConnections = 200;
		/**
		 * The maximum number of connections that can be used by a single route.
		 */
		private int maxPerRouteConnections = 20;
		/**
		 * The socket timeout in millis. Defaults to 10000.
		 */
		private int socketTimeoutMillis = 10000;
		/**
		 * The connection timeout in millis. Defaults to 2000.
		 */
		private int connectTimeoutMillis = 2000;
		/**
		 * The lifetime for the connection pool.
		 */
		private long timeToLive = -1;
		/**
		 * The time unit for timeToLive.
		 */
		private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

		public Host() {
		}

		public Host(int maxTotalConnections, int maxPerRouteConnections,
				int socketTimeoutMillis, int connectTimeoutMillis, long timeToLive,
				TimeUnit timeUnit) {
			this.maxTotalConnections = maxTotalConnections;
			this.maxPerRouteConnections = maxPerRouteConnections;
			this.socketTimeoutMillis = socketTimeoutMillis;
			this.connectTimeoutMillis = connectTimeoutMillis;
			this.timeToLive = timeToLive;
			this.timeUnit = timeUnit;
		}

		public int getMaxTotalConnections() {
			return maxTotalConnections;
		}

		public void setMaxTotalConnections(int maxTotalConnections) {
			this.maxTotalConnections = maxTotalConnections;
		}

		public int getMaxPerRouteConnections() {
			return maxPerRouteConnections;
		}

		public void setMaxPerRouteConnections(int maxPerRouteConnections) {
			this.maxPerRouteConnections = maxPerRouteConnections;
		}

		public int getSocketTimeoutMillis() {
			return socketTimeoutMillis;
		}

		public void setSocketTimeoutMillis(int socketTimeoutMillis) {
			this.socketTimeoutMillis = socketTimeoutMillis;
		}

		public int getConnectTimeoutMillis() {
			return connectTimeoutMillis;
		}

		public void setConnectTimeoutMillis(int connectTimeoutMillis) {
			this.connectTimeoutMillis = connectTimeoutMillis;
		}

		public long getTimeToLive() {
			return timeToLive;
		}

		public void setTimeToLive(long timeToLive) {
			this.timeToLive = timeToLive;
		}

		public TimeUnit getTimeUnit() {
			return timeUnit;
		}

		public void setTimeUnit(TimeUnit timeUnit) {
			this.timeUnit = timeUnit;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Host host = (Host) o;
			return maxTotalConnections == host.maxTotalConnections &&
					maxPerRouteConnections == host.maxPerRouteConnections &&
					socketTimeoutMillis == host.socketTimeoutMillis &&
					connectTimeoutMillis == host.connectTimeoutMillis &&
					timeToLive == host.timeToLive &&
					timeUnit == host.timeUnit;
		}

		@Override
		public int hashCode() {
			return Objects.hash(maxTotalConnections, maxPerRouteConnections, socketTimeoutMillis, connectTimeoutMillis, timeToLive, timeUnit);
		}

		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer("Host{");
			sb.append("maxTotalConnections=").append(maxTotalConnections);
			sb.append(", maxPerRouteConnections=").append(maxPerRouteConnections);
			sb.append(", socketTimeoutMillis=").append(socketTimeoutMillis);
			sb.append(", connectTimeoutMillis=").append(connectTimeoutMillis);
			sb.append(", timeToLive=").append(timeToLive);
			sb.append(", timeUnit=").append(timeUnit);
			sb.append('}');
			return sb.toString();
		}
	}
	
	public static class HystrixSemaphore {
		/**
		 * The maximum number of total semaphores for Hystrix.
		 */
		private int maxSemaphores = 100;

		public HystrixSemaphore() {}

		public HystrixSemaphore(int maxSemaphores) {
			this.maxSemaphores = maxSemaphores;
		}

		public int getMaxSemaphores() {
			return maxSemaphores;
		}

		public void setMaxSemaphores(int maxSemaphores) {
			this.maxSemaphores = maxSemaphores;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			HystrixSemaphore that = (HystrixSemaphore) o;
			return maxSemaphores == that.maxSemaphores;
		}

		@Override
		public int hashCode() {
			return Objects.hash(maxSemaphores);
		}

		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer("HystrixSemaphore{");
			sb.append("maxSemaphores=").append(maxSemaphores);
			sb.append('}');
			return sb.toString();
		}
	}

	public static class HystrixThreadPool {
		/**
		 * Flag to determine whether RibbonCommands should use separate thread pools for hystrix.
		 * By setting to true, RibbonCommands will be executed in a hystrix's thread pool that it is associated with.
		 * Each RibbonCommand will be associated with a thread pool according to its commandKey (serviceId).
		 * As default, all commands will be executed in a single thread pool whose threadPoolKey is "RibbonCommand".
		 * This property is only applicable when using THREAD as ribbonIsolationStrategy
		 */
		private boolean useSeparateThreadPools = false;

		/**
		 * A prefix for HystrixThreadPoolKey of hystrix's thread pool that is allocated to each service Id.
		 * This property is only applicable when using THREAD as ribbonIsolationStrategy and useSeparateThreadPools = true
		 */
		private String threadPoolKeyPrefix = "";

		public boolean isUseSeparateThreadPools() {
			return useSeparateThreadPools;
		}

		public void setUseSeparateThreadPools(boolean useSeparateThreadPools) {
			this.useSeparateThreadPools = useSeparateThreadPools;
		}

		public String getThreadPoolKeyPrefix() {
			return threadPoolKeyPrefix;
		}

		public void setThreadPoolKeyPrefix(String threadPoolKeyPrefix) {
			this.threadPoolKeyPrefix = threadPoolKeyPrefix;
		}
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

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public boolean isStripPrefix() {
		return stripPrefix;
	}

	public void setStripPrefix(boolean stripPrefix) {
		this.stripPrefix = stripPrefix;
	}

	public Boolean getRetryable() {
		return retryable;
	}

	public void setRetryable(Boolean retryable) {
		this.retryable = retryable;
	}

	public Map<String, ZuulRoute> getRoutes() {
		return routes;
	}

	public void setRoutes(Map<String, ZuulRoute> routes) {
		this.routes = routes;
	}

	public boolean isAddProxyHeaders() {
		return addProxyHeaders;
	}

	public void setAddProxyHeaders(boolean addProxyHeaders) {
		this.addProxyHeaders = addProxyHeaders;
	}

	public boolean isAddHostHeader() {
		return addHostHeader;
	}

	public void setAddHostHeader(boolean addHostHeader) {
		this.addHostHeader = addHostHeader;
	}

	public Set<String> getIgnoredServices() {
		return ignoredServices;
	}

	public void setIgnoredServices(Set<String> ignoredServices) {
		this.ignoredServices = ignoredServices;
	}

	public Set<String> getIgnoredPatterns() {
		return ignoredPatterns;
	}

	public void setIgnoredPatterns(Set<String> ignoredPatterns) {
		this.ignoredPatterns = ignoredPatterns;
	}

	public boolean isIgnoreSecurityHeaders() {
		return ignoreSecurityHeaders;
	}

	public void setIgnoreSecurityHeaders(boolean ignoreSecurityHeaders) {
		this.ignoreSecurityHeaders = ignoreSecurityHeaders;
	}

	public boolean isForceOriginalQueryStringEncoding() {
		return forceOriginalQueryStringEncoding;
	}

	public void setForceOriginalQueryStringEncoding(
			boolean forceOriginalQueryStringEncoding) {
		this.forceOriginalQueryStringEncoding = forceOriginalQueryStringEncoding;
	}

	public String getServletPath() {
		return servletPath;
	}

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	public boolean isIgnoreLocalService() {
		return ignoreLocalService;
	}

	public void setIgnoreLocalService(boolean ignoreLocalService) {
		this.ignoreLocalService = ignoreLocalService;
	}

	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
	}

	public boolean isTraceRequestBody() {
		return traceRequestBody;
	}

	public void setTraceRequestBody(boolean traceRequestBody) {
		this.traceRequestBody = traceRequestBody;
	}

	public boolean isRemoveSemicolonContent() {
		return removeSemicolonContent;
	}

	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.removeSemicolonContent = removeSemicolonContent;
	}

	public Set<String> getSensitiveHeaders() {
		return sensitiveHeaders;
	}

	public void setSensitiveHeaders(Set<String> sensitiveHeaders) {
		this.sensitiveHeaders = sensitiveHeaders;
	}

	public boolean isSslHostnameValidationEnabled() {
		return sslHostnameValidationEnabled;
	}

	public void setSslHostnameValidationEnabled(boolean sslHostnameValidationEnabled) {
		this.sslHostnameValidationEnabled = sslHostnameValidationEnabled;
	}

	public ExecutionIsolationStrategy getRibbonIsolationStrategy() {
		return ribbonIsolationStrategy;
	}

	public void setRibbonIsolationStrategy(
			ExecutionIsolationStrategy ribbonIsolationStrategy) {
		this.ribbonIsolationStrategy = ribbonIsolationStrategy;
	}

	public HystrixSemaphore getSemaphore() {
		return semaphore;
	}

	public void setSemaphore(HystrixSemaphore semaphore) {
		this.semaphore = semaphore;
	}

	public HystrixThreadPool getThreadPool() {
		return threadPool;
	}

	public void setThreadPool(HystrixThreadPool threadPool) {
		this.threadPool = threadPool;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ZuulProperties that = (ZuulProperties) o;
		return addHostHeader == that.addHostHeader &&
				addProxyHeaders == that.addProxyHeaders &&
				forceOriginalQueryStringEncoding == that.forceOriginalQueryStringEncoding &&
				Objects.equals(host, that.host) &&
				Objects.equals(ignoredHeaders, that.ignoredHeaders) &&
				Objects.equals(ignoredPatterns, that.ignoredPatterns) &&
				Objects.equals(ignoredServices, that.ignoredServices) &&
				ignoreLocalService == that.ignoreLocalService &&
				ignoreSecurityHeaders == that.ignoreSecurityHeaders &&
				Objects.equals(prefix, that.prefix) &&
				removeSemicolonContent == that.removeSemicolonContent &&
				Objects.equals(retryable, that.retryable) &&
				Objects.equals(ribbonIsolationStrategy, that.ribbonIsolationStrategy) &&
				Objects.equals(routes, that.routes) &&
				Objects.equals(semaphore, that.semaphore) &&
				Objects.equals(sensitiveHeaders, that.sensitiveHeaders) &&
				Objects.equals(servletPath, that.servletPath) &&
				sslHostnameValidationEnabled == that.sslHostnameValidationEnabled &&
				stripPrefix == that.stripPrefix &&
				Objects.equals(threadPool, that.threadPool) &&
				traceRequestBody == that.traceRequestBody;
	}

	@Override
	public int hashCode() {
		return Objects.hash(addHostHeader, addProxyHeaders, forceOriginalQueryStringEncoding,
				host, ignoredHeaders, ignoredPatterns, ignoredServices, ignoreLocalService,
				ignoreSecurityHeaders, prefix, removeSemicolonContent, retryable,
				ribbonIsolationStrategy, routes, semaphore, sensitiveHeaders, servletPath,
				sslHostnameValidationEnabled, stripPrefix, threadPool, traceRequestBody);
	}

	@Override
	public String toString() {
		return new StringBuilder("ZuulProperties{")
				.append("prefix='").append(prefix).append("', ")
				.append("stripPrefix=").append(stripPrefix).append(", ")
				.append("retryable=").append(retryable).append(", ")
				.append("routes=").append(routes).append(", ")
				.append("addProxyHeaders=").append(addProxyHeaders).append(", ")
				.append("addHostHeader=").append(addHostHeader).append(", ")
				.append("ignoredServices=").append(ignoredServices).append(", ")
				.append("ignoredPatterns=").append(ignoredPatterns).append(", ")
				.append("ignoredHeaders=").append(ignoredHeaders).append(", ")
				.append("ignoreSecurityHeaders=").append(ignoreSecurityHeaders).append(", ")
				.append("forceOriginalQueryStringEncoding=").append(forceOriginalQueryStringEncoding).append(", ")
				.append("servletPath='").append(servletPath).append("', ")
				.append("ignoreLocalService=").append(ignoreLocalService).append(", ")
				.append("host=").append(host).append(", ")
				.append("traceRequestBody=").append(traceRequestBody).append(", ")
				.append("removeSemicolonContent=").append(removeSemicolonContent).append(", ")
				.append("sensitiveHeaders=").append(sensitiveHeaders).append(", ")
				.append("sslHostnameValidationEnabled=").append(sslHostnameValidationEnabled).append(", ")
				.append("ribbonIsolationStrategy=").append(ribbonIsolationStrategy).append(", ")
				.append("semaphore=").append(semaphore).append(", ")
				.append("threadPool=").append(threadPool).append(", ")
				.append("}").toString();
	}

}
