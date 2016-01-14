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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

	@NestedConfigurationProperty
	private Map<String, ZuulRoute> routes = new LinkedHashMap<>();

	private boolean addProxyHeaders = true;

	private List<String> ignoredServices = new ArrayList<>();

	private List<String> ignoredPatterns = new ArrayList<>();

	private String servletPath = "/zuul";

	private boolean ignoreLocalService = true;

	private RegexMapper regexMapper = new RegexMapper();

	@PostConstruct
	public void init() {
		for (Entry<String, ZuulRoute> entry : this.routes.entrySet()) {
			ZuulRoute value = entry.getValue();
			if (!StringUtils.hasText(value.getLocation())) {
				value.setServiceId(entry.getKey());
			}
			if (!StringUtils.hasText(value.getId())) {
				value.setId(entry.getKey());
			}
			if (!StringUtils.hasText(value.getPath())) {
				value.setPath("/" + entry.getKey() + "/**");
			}
		}
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class RegexMapper {
		private boolean enabled = false;

		private String servicePattern = "(?<name>.*)-(?<version>v.*$)";

		private String routePattern = "${version}/${name}";
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
