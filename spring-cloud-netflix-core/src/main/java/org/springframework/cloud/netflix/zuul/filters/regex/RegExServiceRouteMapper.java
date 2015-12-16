package org.springframework.cloud.netflix.zuul.filters.regex;

import org.springframework.cloud.netflix.zuul.filters.ServiceRouteMapper;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Stéphane Leroy
 *
 * This service route mapper use Java 7 RegEx named group feature to rewrite a discovered service Id into a route.
 *
 * Ex : If we want to map service Id [rest-service-v1] to /v1/rest-service/** route
 * service pattern : "(?<name>.*)-(?<version>v.*$)"
 * route pattern : "${version}/${name}"
 *
 * /!\ This implementation use Matcher.replaceFirst so only one match will be replace.
 */
public class RegExServiceRouteMapper implements ServiceRouteMapper {

    /**
     * A RegExp Pattern that extract needed information from a service ID.
     * Ex : "(?<name>.*)-(?<version>v.*$)"
     */
    private Pattern servicePattern;
    /**
     * A RegExp that refer to named groups define in servicePattern.
     * Ex : "${version}/${name}"
     */
    private String routePattern;

    public RegExServiceRouteMapper(String servicePattern, String routePattern) {
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
        Matcher matcher = servicePattern.matcher(serviceId);
        String route = matcher.replaceFirst(routePattern);
        route = cleanRoute(route);
        return (StringUtils.hasText(route)?route:serviceId);
    }

    /**
     * Route with regex and replace can be a bit messy when used with conditional named group.
     * We clean here first and trailing '/' and remove multiple consecutive '/'
     * @param route
     * @return
     */
    private String cleanRoute(final String route) {
        String routeToClean = route.replaceAll("/{2,}", "/");
        if(routeToClean.startsWith("/")) {
            routeToClean = routeToClean.substring(1);
        }
        if(routeToClean.endsWith("/")) {
            routeToClean = routeToClean.substring(0, routeToClean.length()-1);
        }
        return routeToClean;
    }
}
