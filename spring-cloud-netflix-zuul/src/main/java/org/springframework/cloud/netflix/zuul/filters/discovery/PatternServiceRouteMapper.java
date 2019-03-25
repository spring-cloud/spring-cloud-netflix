/*
 * Copyright 2015-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul.filters.discovery;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

/**
 * @author Stéphane Leroy
 *
 * This service route mapper use Java 7 RegEx named group feature to rewrite a discovered
 * service Id into a route.
 *
 * Ex : If we want to map service Id <code>[rest-service-v1]</code> to
 * <code>/v1/rest-service/**</code> route service pattern :
 * <code>"(?<name>.*)-(?<version>v.*$)"</code> route pattern :
 * <code>"${version}/${name}"</code>
 *
 * This implementation uses <code>Matcher.replaceFirst</code> so only one match will be
 * replaced.
 */
public class PatternServiceRouteMapper implements ServiceRouteMapper {

	private static final Pattern MULTIPLE_SLASH_PATTERN = Pattern.compile("/{2,}");

	/**
	 * A RegExp Pattern that extract needed information from a service ID. Ex :
	 * "(?<name>.*)-(?<version>v.*$)"
	 */
	private Pattern servicePattern;

	/**
	 * A RegExp that refer to named groups define in servicePattern. Ex :
	 * "${version}/${name}"
	 */
	private String routePattern;

	public PatternServiceRouteMapper(String servicePattern, String routePattern) {
		this.servicePattern = Pattern.compile(servicePattern);
		this.routePattern = routePattern;
	}

	/**
	 * Use servicePattern to extract groups and routePattern to construct the route.
	 *
	 * If there is no matches, the serviceId is returned.
	 * @param serviceId service discovered name
	 * @return route path
	 */
	@Override
	public String apply(String serviceId) {
		Matcher matcher = this.servicePattern.matcher(serviceId);
		String route = matcher.replaceFirst(this.routePattern);
		route = cleanRoute(route);
		return (StringUtils.hasText(route) ? route : serviceId);
	}

	/**
	 * Route with regex and replace can be a bit messy when used with conditional named
	 * group. We clean here first and trailing '/' and remove multiple consecutive '/'.
	 * @param route a {@link String} representation of the route to be cleaned
	 * @return cleaned up route {@link String}
	 */
	private String cleanRoute(final String route) {
		String routeToClean = MULTIPLE_SLASH_PATTERN.matcher(route).replaceAll("/");
		if (routeToClean.startsWith("/")) {
			routeToClean = routeToClean.substring(1);
		}
		if (routeToClean.endsWith("/")) {
			routeToClean = routeToClean.substring(0, routeToClean.length() - 1);
		}
		return routeToClean;
	}

}
