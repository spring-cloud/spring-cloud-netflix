package org.springframework.cloud.netflix.zuul.filters;

/**
 * @author Stéphane LEROY
 *
 * Provide a way to apply convention between routes and discovered services name.
 *
 */
public interface ServiceRouteMapper {

    /**
     * Take a service Id (its discovered name) and return a route path.
     *
     * @param serviceId service discovered name
     * @return route path
     */
    String apply(String serviceId);
}
