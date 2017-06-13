package org.springframework.cloud.netflix.zuul.filters.discovery;

import com.netflix.zuul.context.RequestContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Stéphane Leroy
 */
public class PatternServiceRouteMapperTests {

	/**
	 * Service pattern that follow convention {domain}-{name}-{version}. The name is
	 * optional
	 */
	public static final String SERVICE_PATTERN = "(?<domain>^\\w+)(-(?<name>\\w+)-|-)(?<version>v\\d+$)";
	public static final String ROUTE_PATTERN = "${version}/${domain}/${name}";

	@Before
	public void setTestRequestcontext() {
		RequestContext context = new RequestContext();
		RequestContext.testSetCurrentContext(context);
	}

	@After
	public void clear() {
		RequestContext.getCurrentContext().clear();
	}

	@Test
	public void test_return_mapped_route_if_serviceid_matches() {
		PatternServiceRouteMapper toTest = new PatternServiceRouteMapper(SERVICE_PATTERN,
				ROUTE_PATTERN);

		assertEquals("service version convention", "v1/rest/service",
				toTest.apply("rest-service-v1"));
	}

	@Test
	public void test_return_serviceid_if_no_matches() {
		PatternServiceRouteMapper toTest = new PatternServiceRouteMapper(SERVICE_PATTERN,
				ROUTE_PATTERN);

		// No version here
		assertEquals("No matches for this service id", "rest-service",
				toTest.apply("rest-service"));
	}

	@Test
	public void test_route_should_be_cleaned_before_returned() {
		// Messy patterns
		PatternServiceRouteMapper toTest = new PatternServiceRouteMapper(
				SERVICE_PATTERN + "(?<nevermatch>.)?",
				"/${version}/${nevermatch}/${domain}/${name}/");
		assertEquals("No matches for this service id", "v1/domain/service",
				toTest.apply("domain-service-v1"));
		assertEquals("No matches for this service id", "v1/domain",
				toTest.apply("domain-v1"));
	}
}
