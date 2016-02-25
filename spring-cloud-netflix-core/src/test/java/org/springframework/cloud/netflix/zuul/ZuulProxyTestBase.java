package org.springframework.cloud.netflix.zuul;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import static org.junit.Assert.assertEquals;

/**
 * @author Spencer Gibb
 */
public abstract class ZuulProxyTestBase {

	@Value("${local.server.port}")
	protected int port;

	@Autowired
	protected DiscoveryClientRouteLocator routes;

	@Autowired
	protected RoutesEndpoint endpoint;

	@Autowired
	protected RibbonCommandFactory<?> ribbonCommandFactory;

	@Before
	public void setTestRequestcontext() {
		RequestContext.testSetCurrentContext(null);
		RequestContext.getCurrentContext().unset();
	}

	protected String getRoute(String path) {
		for (Route route : this.routes.getRoutes()) {
			if (path.equals(route.getFullPath())) {
				return route.getLocation();
			}
		}
		return null;
	}

	@Test
	public void bindRouteUsingPhysicalRoute() {
		assertEquals("http://localhost:7777/local", getRoute("/test/**"));
	}

	@Test
	public void bindRouteUsingOnlyPath() {
		assertEquals("simple", getRoute("/simple/**"));
	}

	@Test
	public void getOnSelfViaRibbonRoutingFilter() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/local/1", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Gotten 1!", result.getBody());
	}

	@Test
	public void deleteOnSelfViaSimpleHostRoutingFilter() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port + "/local");
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/1", HttpMethod.DELETE,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Deleted 1!", result.getBody());
	}

	@Test
	public void stripPrefixFalseAppendsPath() {
		this.routes.addRoute(new ZuulProperties.ZuulRoute("strip", "/strip/**", "strip",
				"http://localhost:" + this.port + "/local", false, false, null));
		this.endpoint.reset();
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/strip", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		// Prefix not stripped to it goes to /local/strip
		assertEquals("Gotten strip!", result.getBody());
	}

	@Test
	public void testNotFoundFromApp() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/local/notfound",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
	}

	@Test
	public void testNotFoundOnProxy() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/myinvalidpath", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
	}

	@Test
	public void getSecondLevel() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/another/twolevel/local/1",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Gotten 1!", result.getBody());
	}

	@Test
	public void ribbonRouteWithSpace() {
		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/simple/spa ce", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Hello space", result.getBody());
	}

	@Test
	public void simpleHostRouteWithSpace() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port);
		this.endpoint.reset();

		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/self/spa ce", HttpMethod.GET,
				new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Hello space", result.getBody());
	}

	@Test
	public void simpleHostRouteWithOriginalQueryString() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port);
		this.endpoint.reset();

		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port
						+ "/self/qstring?original=value1&original=value2",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Received {original=[value1, value2]}", result.getBody());
	}

	@Test
	public void simpleHostRouteWithOverriddenQString() {
		this.routes.addRoute("/self/**", "http://localhost:" + this.port);
		this.endpoint.reset();

		ResponseEntity<String> result = new TestRestTemplate().exchange(
				"http://localhost:" + this.port
						+ "/self/qstring?override=true&different=key",
				HttpMethod.GET, new HttpEntity<>((Void) null), String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Received {key=[overridden]}", result.getBody());
	}

	protected static abstract class AbstractZuulProxyApplication {

		@RequestMapping("/testing123")
		public String testing123() {
			throw new RuntimeException("myerror");
		}

		@RequestMapping("/local")
		public String local() {
			return "Hello local";
		}

		@RequestMapping(value = "/local/{id}", method = RequestMethod.DELETE)
		public String delete(@PathVariable String id) {
			return "Deleted " + id + "!";
		}

		@RequestMapping(value = "/local/{id}", method = RequestMethod.GET)
		public ResponseEntity get(@PathVariable String id) {
			if ("notfound".equalsIgnoreCase(id)) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok("Gotten " + id + "!");
		}

		@RequestMapping(value = "/local/{id}", method = RequestMethod.POST)
		public String post(@PathVariable String id, @RequestBody String body) {
			return "Posted " + id + "!";
		}

		@RequestMapping(value = "/qstring")
		public String qstring(@RequestParam MultiValueMap<String, String> params) {
			return "Received " + params.toString();
		}

		@RequestMapping("/")
		public String home() {
			return "Hello world";
		}

		@RequestMapping("/spa ce")
		public String space() {
			return "Hello space";
		}

		@Bean
		public ZuulFilter sampleFilter() {
			return new ZuulFilter() {
				@Override
				public String filterType() {
					return "pre";
				}

				@Override
				public boolean shouldFilter() {
					return true;
				}

				@Override
				public Object run() {
					if (RequestContext.getCurrentContext().getRequest().getParameterMap()
							.containsKey("override")) {
						Map<String, List<String>> overridden = new HashMap<>();
						overridden.put("key", Arrays.asList("overridden"));
						RequestContext.getCurrentContext()
								.setRequestQueryParams(overridden);
					}
					return null;
				}

				@Override
				public int filterOrder() {
					return 0;
				}
			};
		}
	}
}

// Load balancer with fixed server list for "simple" pointing to localhost
@Configuration
class SimpleRibbonClientConfiguration {

	@Value("${local.server.port}")
	private int port;

	@Bean
	public ServerList<Server> ribbonServerList() {
		return new StaticServerList<>(new Server("localhost", this.port));
	}

}

@Configuration
class AnotherRibbonClientConfiguration {

	@Value("${local.server.port}")
	private int port;

	@Bean
	public ServerList<Server> ribbonServerList() {
		return new StaticServerList<>(new Server("localhost", this.port));
	}

}