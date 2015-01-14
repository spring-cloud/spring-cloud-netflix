package org.springframework.cloud.netflix.zuul.filters.pre;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.ProxyRouteLocator;
import org.springframework.cloud.netflix.zuul.ZuulProperties;
import org.springframework.cloud.netflix.zuul.ZuulProperties.ZuulRoute;
import org.springframework.mock.web.MockHttpServletRequest;

import com.netflix.util.Pair;
import com.netflix.zuul.context.RequestContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Dave Syer
 *
 */
public class PreDecorationFilterTests {

	private PreDecorationFilter filter;

	@Mock
	DiscoveryClient discovery;

	private ZuulProperties properties = new ZuulProperties();

	private ProxyRouteLocator routeLocator;

	private MockHttpServletRequest request = new MockHttpServletRequest();

	@Before
	public void init() {
		initMocks(this);
		this.routeLocator = new ProxyRouteLocator(this.discovery, this.properties);
		this.filter = new PreDecorationFilter(this.routeLocator, this.properties);
		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.setRequest(this.request);
	}

	@Test
	public void basicProperties() throws Exception {
		assertEquals(5, this.filter.filterOrder());
		assertEquals(true, this.filter.shouldFilter());
		assertEquals("pre", this.filter.filterType());
	}

	@Test
	public void prefixRouteAddsHeader() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.request.setRequestURI("/api/foo/1");
		this.routeLocator.addRoute(new ZuulRoute("foo", "/foo/**", "foo", null, false));
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/foo/1", ctx.get("requestURI"));
		assertEquals("localhost:80", ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("/api", ctx.getZuulRequestHeaders().get("x-forwarded-prefix"));
		assertEquals("foo", getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"));
	}

	@Test
	public void prefixRouteWithRouteStrippingAddsHeader() throws Exception {
		this.properties.setPrefix("/api");
		this.properties.setStripPrefix(true);
		this.request.setRequestURI("/api/foo/1");
		this.routeLocator.addRoute("/foo/**", "foo");
		this.filter.run();
		RequestContext ctx = RequestContext.getCurrentContext();
		assertEquals("/1", ctx.get("requestURI"));
		assertEquals("localhost:80", ctx.getZuulRequestHeaders().get("x-forwarded-host"));
		assertEquals("/api/foo", ctx.getZuulRequestHeaders().get("x-forwarded-prefix"));
		assertEquals("foo", getHeader(ctx.getOriginResponseHeaders(), "x-zuul-serviceid"));
	}

	private Object getHeader(List<Pair<String, String>> headers, String key) {
		String value = null;
		for (Pair<String, String> pair : headers) {
			if (pair.first().toLowerCase().equals(key.toLowerCase())) {
				value = pair.second();
				break;
			}
		}
		return value;
	}

}
