package org.springframework.cloud.netflix.zuul.filters;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;

/**
 * @author Tom Cawley
 */
public class SimpleRouteLocatorTests {
	private ZuulProperties zuul = new ZuulProperties();

	public SimpleRouteLocatorTests() {

	}

	@Test
	public void test_getRoutesDefaultRouteAcceptor() {
		RouteLocator locator = new SimpleRouteLocator("/", this.zuul);
		this.zuul.getRoutes().clear();
		this.zuul.getRoutes().put("foo", new ZuulRoute("foo", "/foo"));

		assertThat(locator.getRoutes(), hasItem(createRoute("foo", "foo")));
	}

	@Test
	public void test_getRoutesFilterRouteAcceptor() {
		RouteLocator locator = new FilteringRouteLocator("/", this.zuul);
		this.zuul.getRoutes().clear();
		this.zuul.getRoutes().put("foo", new ZuulRoute("foo", "/foo"));
		this.zuul.getRoutes().put("bar", new ZuulRoute("bar", "/bar"));

		assertThat(locator.getRoutes(), hasItem(createRoute("bar", "bar")));
	}

	private Route createRoute(String id, String path) {
		return new Route(id, path, "/"+path, "", false, null);
	}

	private static class FilteringRouteLocator extends SimpleRouteLocator {
		public FilteringRouteLocator(final String servletPath, final ZuulProperties properties) {

			super(servletPath, properties);
		}

		@Override public boolean acceptRoute(final ZuulRoute route) {

			return !(route.getId().equals("foo"));
		}
	}
}