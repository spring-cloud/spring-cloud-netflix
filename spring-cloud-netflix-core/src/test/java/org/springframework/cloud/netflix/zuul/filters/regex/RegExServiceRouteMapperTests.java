package org.springframework.cloud.netflix.zuul.filters.regex;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Stéphane Leroy
 */
public class RegExServiceRouteMapperTests {

    public static final String SERVICE_PATTERN = "(?<name>.*)-(?<version>v.*$)";
    public static final String ROUTE_PATTERN = "${version}/${name}";

    @Test
    public void test_return_mapped_route_if_serviceid_matches() {
        RegExServiceRouteMapper toTest = new RegExServiceRouteMapper(SERVICE_PATTERN, ROUTE_PATTERN);

        assertEquals("service version convention", "v1/rest-service", toTest.apply("rest-service-v1"));
    }

    @Test
    public void test_return_serviceid_if_no_matches() {
        RegExServiceRouteMapper toTest = new RegExServiceRouteMapper(SERVICE_PATTERN, ROUTE_PATTERN);

        assertEquals("No matches for this service id", "rest-service", toTest.apply("rest-service"));
    }
}
