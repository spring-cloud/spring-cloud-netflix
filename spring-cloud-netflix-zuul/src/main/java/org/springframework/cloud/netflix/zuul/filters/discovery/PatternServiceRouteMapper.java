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
	 *
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
	 * group. We clean here first and trailing '/' and remove multiple consecutive '/'
	 * @param route
	 * @return
	 */
	private String cleanRoute(final String route) {
		String routeToClean = route.replaceAll("/{2,}", "/");
		if (routeToClean.startsWith("/")) {
			routeToClean = routeToClean.substring(1);
		}
		if (routeToClean.endsWith("/")) {
			routeToClean = routeToClean.substring(0, routeToClean.length() - 1);
		}
		return routeToClean;
	}
}
