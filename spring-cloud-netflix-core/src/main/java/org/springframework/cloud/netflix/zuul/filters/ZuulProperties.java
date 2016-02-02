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

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
@ConfigurationProperties("zuul")
public class ZuulProperties {

	private String prefix = "";

	private boolean stripPrefix = true;

	private Boolean retryable;

	private Map<String, ZuulRoute> routes = new LinkedHashMap<>();

	private boolean addProxyHeaders = true;

	private List<String> ignoredServices = new ArrayList<>();

	private List<String> ignoredPatterns = new ArrayList<>();

	private String servletPath = "/zuul";

	private boolean ignoreLocalService = true;

	public ZuulProperties() {
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

	public String getPrefix() {
		return this.prefix;
	}

	public boolean isStripPrefix() {
		return this.stripPrefix;
	}

	public Boolean getRetryable() {
		return this.retryable;
	}

	public Map<String, ZuulRoute> getRoutes() {
		return this.routes;
	}

	public boolean isAddProxyHeaders() {
		return this.addProxyHeaders;
	}

	public List<String> getIgnoredServices() {
		return this.ignoredServices;
	}

	public List<String> getIgnoredPatterns() {
		return this.ignoredPatterns;
	}

	public String getServletPath() {
		return this.servletPath;
	}

	public boolean isIgnoreLocalService() {
		return this.ignoreLocalService;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setStripPrefix(boolean stripPrefix) {
		this.stripPrefix = stripPrefix;
	}

	public void setRetryable(Boolean retryable) {
		this.retryable = retryable;
	}

	public void setRoutes(Map<String, ZuulRoute> routes) {
		this.routes = routes;
	}

	public void setAddProxyHeaders(boolean addProxyHeaders) {
		this.addProxyHeaders = addProxyHeaders;
	}

	public void setIgnoredServices(List<String> ignoredServices) {
		this.ignoredServices = ignoredServices;
	}

	public void setIgnoredPatterns(List<String> ignoredPatterns) {
		this.ignoredPatterns = ignoredPatterns;
	}

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	public void setIgnoreLocalService(boolean ignoreLocalService) {
		this.ignoreLocalService = ignoreLocalService;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof ZuulProperties))
			return false;
		final ZuulProperties other = (ZuulProperties) o;
		if (!other.canEqual((Object) this))
			return false;
		final Object this$prefix = this.prefix;
		final Object other$prefix = other.prefix;
		if (this$prefix == null ?
				other$prefix != null :
				!this$prefix.equals(other$prefix))
			return false;
		if (this.stripPrefix != other.stripPrefix)
			return false;
		final Object this$retryable = this.retryable;
		final Object other$retryable = other.retryable;
		if (this$retryable == null ?
				other$retryable != null :
				!this$retryable.equals(other$retryable))
			return false;
		final Object this$routes = this.routes;
		final Object other$routes = other.routes;
		if (this$routes == null ?
				other$routes != null :
				!this$routes.equals(other$routes))
			return false;
		if (this.addProxyHeaders != other.addProxyHeaders)
			return false;
		final Object this$ignoredServices = this.ignoredServices;
		final Object other$ignoredServices = other.ignoredServices;
		if (this$ignoredServices == null ?
				other$ignoredServices != null :
				!this$ignoredServices.equals(other$ignoredServices))
			return false;
		final Object this$ignoredPatterns = this.ignoredPatterns;
		final Object other$ignoredPatterns = other.ignoredPatterns;
		if (this$ignoredPatterns == null ?
				other$ignoredPatterns != null :
				!this$ignoredPatterns.equals(other$ignoredPatterns))
			return false;
		final Object this$servletPath = this.servletPath;
		final Object other$servletPath = other.servletPath;
		if (this$servletPath == null ?
				other$servletPath != null :
				!this$servletPath.equals(other$servletPath))
			return false;
		if (this.ignoreLocalService != other.ignoreLocalService)
			return false;
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $prefix = this.prefix;
		result = result * PRIME + ($prefix == null ? 0 : $prefix.hashCode());
		result = result * PRIME + (this.stripPrefix ? 79 : 97);
		final Object $retryable = this.retryable;
		result = result * PRIME + ($retryable == null ? 0 : $retryable.hashCode());
		final Object $routes = this.routes;
		result = result * PRIME + ($routes == null ? 0 : $routes.hashCode());
		result = result * PRIME + (this.addProxyHeaders ? 79 : 97);
		final Object $ignoredServices = this.ignoredServices;
		result = result * PRIME + ($ignoredServices == null ?
				0 :
				$ignoredServices.hashCode());
		final Object $ignoredPatterns = this.ignoredPatterns;
		result = result * PRIME + ($ignoredPatterns == null ?
				0 :
				$ignoredPatterns.hashCode());
		final Object $servletPath = this.servletPath;
		result = result * PRIME + ($servletPath == null ? 0 : $servletPath.hashCode());
		result = result * PRIME + (this.ignoreLocalService ? 79 : 97);
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof ZuulProperties;
	}

	public String toString() {
		return "org.springframework.cloud.netflix.zuul.filters.ZuulProperties(prefix="
				+ this.prefix + ", stripPrefix=" + this.stripPrefix + ", retryable="
				+ this.retryable + ", routes=" + this.routes + ", addProxyHeaders="
				+ this.addProxyHeaders + ", ignoredServices=" + this.ignoredServices
				+ ", ignoredPatterns=" + this.ignoredPatterns + ", servletPath="
				+ this.servletPath + ", ignoreLocalService=" + this.ignoreLocalService
				+ ")";
	}

	public static class ZuulRoute {

		private String id;

		private String path;

		private String serviceId;

		private String url;

		private boolean stripPrefix = true;

		private Boolean retryable;

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

		@java.beans.ConstructorProperties(
				{ "id", "path", "serviceId", "url", "stripPrefix", "retryable" })
		public ZuulRoute(String id, String path, String serviceId, String url,
				boolean stripPrefix, Boolean retryable) {
			this.id = id;
			this.path = path;
			this.serviceId = serviceId;
			this.url = url;
			this.stripPrefix = stripPrefix;
			this.retryable = retryable;
		}

		public ZuulRoute() {
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
			return new Route(this.id, this.path, getLocation(), prefix, this.retryable);
		}

		public String getId() {
			return this.id;
		}

		public String getPath() {
			return this.path;
		}

		public String getServiceId() {
			return this.serviceId;
		}

		public String getUrl() {
			return this.url;
		}

		public boolean isStripPrefix() {
			return this.stripPrefix;
		}

		public Boolean getRetryable() {
			return this.retryable;
		}

		public void setId(String id) {
			this.id = id;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public void setServiceId(String serviceId) {
			this.serviceId = serviceId;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public void setStripPrefix(boolean stripPrefix) {
			this.stripPrefix = stripPrefix;
		}

		public void setRetryable(Boolean retryable) {
			this.retryable = retryable;
		}

		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof ZuulRoute))
				return false;
			final ZuulRoute other = (ZuulRoute) o;
			if (!other.canEqual((Object) this))
				return false;
			final Object this$id = this.id;
			final Object other$id = other.id;
			if (this$id == null ? other$id != null : !this$id.equals(other$id))
				return false;
			final Object this$path = this.path;
			final Object other$path = other.path;
			if (this$path == null ? other$path != null : !this$path.equals(other$path))
				return false;
			final Object this$serviceId = this.serviceId;
			final Object other$serviceId = other.serviceId;
			if (this$serviceId == null ?
					other$serviceId != null :
					!this$serviceId.equals(other$serviceId))
				return false;
			final Object this$url = this.url;
			final Object other$url = other.url;
			if (this$url == null ? other$url != null : !this$url.equals(other$url))
				return false;
			if (this.stripPrefix != other.stripPrefix)
				return false;
			final Object this$retryable = this.retryable;
			final Object other$retryable = other.retryable;
			if (this$retryable == null ?
					other$retryable != null :
					!this$retryable.equals(other$retryable))
				return false;
			return true;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $id = this.id;
			result = result * PRIME + ($id == null ? 0 : $id.hashCode());
			final Object $path = this.path;
			result = result * PRIME + ($path == null ? 0 : $path.hashCode());
			final Object $serviceId = this.serviceId;
			result = result * PRIME + ($serviceId == null ? 0 : $serviceId.hashCode());
			final Object $url = this.url;
			result = result * PRIME + ($url == null ? 0 : $url.hashCode());
			result = result * PRIME + (this.stripPrefix ? 79 : 97);
			final Object $retryable = this.retryable;
			result = result * PRIME + ($retryable == null ? 0 : $retryable.hashCode());
			return result;
		}

		protected boolean canEqual(Object other) {
			return other instanceof ZuulRoute;
		}

		public String toString() {
			return "org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute(id="
					+ this.id + ", path=" + this.path + ", serviceId=" + this.serviceId
					+ ", url=" + this.url + ", stripPrefix=" + this.stripPrefix
					+ ", retryable=" + this.retryable + ")";
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

}
