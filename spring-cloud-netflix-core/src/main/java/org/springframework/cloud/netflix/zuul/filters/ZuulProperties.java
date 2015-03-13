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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Data
@ConfigurationProperties("zuul")
public class ZuulProperties {

	private String prefix = "";

	private boolean stripPrefix = true;

	private Boolean retryable;

	private Map<String, ZuulRoute> routes = new LinkedHashMap<String, ZuulRoute>();

	private boolean addProxyHeaders = true;

	private List<String> ignoredServices = new ArrayList<String>();

	private String servletPath = "/zuul";

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

		public ZuulRoute(String text) {
			String location = null;
			String path = text;
			if (text.contains("=")) {
				String[] values = StringUtils.trimArrayElements(StringUtils.split(text,
						"="));
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
